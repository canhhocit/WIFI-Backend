package com.wificaptive.upload.controller;

import com.wificaptive.upload.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Same-origin media streamer for Captive Portal guests (iPhone iOS CNA, Android CaptivePortalLogin, Laptop).
 * Supports RFC 7233 HTTP Byte-Range (206 Partial Content) required by iOS Safari / WebKit for HTML5 <video>,
 * transparently serving from local disk or MinIO object storage through the portal domain (/uploads/**)
 * so media is never blocked by the router's pre-authentication Walled Garden.
 */
@RestController
public class MediaStreamController {

    private static final Logger log = LoggerFactory.getLogger(MediaStreamController.class);
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final long MAX_CHUNK_BYTES = 4L * 1024 * 1024; // 4MB per range chunk for smooth mobile streaming

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;
    private final HttpClient httpClient;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir = "uploads";

    // In-memory cache for proxied YouTube thumbnails (avoids external requests inside captive portal Walled Garden)
    private final Map<String, CachedBinary> ytThumbCache = new ConcurrentHashMap<>();
    // In-memory cache for resolved direct YouTube MP4 stream URLs (itag 18)
    private final Map<String, CachedStreamUrl> ytStreamUrlCache = new ConcurrentHashMap<>();
    // Track in-flight background downloads so we don't duplicate downloads for the same videoId
    private final java.util.Set<String> activeYtDownloads = ConcurrentHashMap.newKeySet();

    private static class CachedStreamUrl {
        final String url;
        final Instant expiresAt;

        CachedStreamUrl(String url, Instant expiresAt) {
            this.url = url;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private static class CachedBinary {
        final byte[] data;
        final String contentType;
        final Instant expiresAt;

        CachedBinary(byte[] data, String contentType, Instant expiresAt) {
            this.data = data;
            this.contentType = contentType;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Autowired
    public MediaStreamController(
            MinioProperties minioProperties,
            @Autowired(required = false) MinioClient minioClient,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
        this.redisTemplate = redisTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public MediaStreamController(MinioProperties minioProperties, MinioClient minioClient) {
        this(minioProperties, minioClient, null);
    }

    @GetMapping({"/uploads/{category}/{filename:.+}", "/api/portal/media/{category}/{filename:.+}"})
    public ResponseEntity<StreamingResponseBody> streamMedia(
            @PathVariable("category") String category,
            @PathVariable("filename") String filename,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {
        String cleanCategory = sanitizeSegment(category);
        String cleanFilename = sanitizeSegment(filename);
        if (cleanCategory.isEmpty() || cleanFilename.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String defaultMime = inferContentType(cleanFilename);

        // 1. Check local filesystem first
        Path localPath = Paths.get(baseUploadDir, cleanCategory, cleanFilename).toAbsolutePath().normalize();
        if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
            try {
                return streamFromLocalFile(localPath, rangeHeader, defaultMime);
            } catch (IOException e) {
                log.warn("Error streaming local media {}/{}: {}", cleanCategory, cleanFilename, e.getMessage());
            }
        }

        // 2. Check internal MinIO client if enabled
        String objectKey = cleanCategory + "/" + cleanFilename;
        if (minioClient != null && minioProperties != null && minioProperties.isEnabled()) {
            try {
                return streamFromMinio(objectKey, rangeHeader, defaultMime);
            } catch (Exception e) {
                log.debug("Object {} not found in direct MinIO client, trying public URL fallback: {}", objectKey, e.getMessage());
            }
        }

        // 3. Fallback: proxy from MinIO public URL (allows local dev or standby nodes to stream MinIO assets same-origin)
        if (minioProperties != null && StringUtils.hasText(minioProperties.getPublicUrl())) {
            String baseUrl = minioProperties.getPublicUrl().endsWith("/")
                    ? minioProperties.getPublicUrl().substring(0, minioProperties.getPublicUrl().length() - 1)
                    : minioProperties.getPublicUrl();
            String remoteUrl = baseUrl + "/" + objectKey;
            try {
                ResponseEntity<StreamingResponseBody> remoteResp = streamFromRemoteUrl(remoteUrl, rangeHeader, defaultMime);
                if (remoteResp.getStatusCode().is2xxSuccessful()) {
                    return remoteResp;
                }
            } catch (Exception e) {
                log.debug("Remote MinIO public URL miss for {}: {}", remoteUrl, e.getMessage());
            }
        }

        // 4. Automatic YouTube-to-MP4 same-origin resolution & background caching (for filenames matching yt_<videoId>.mp4)
        // Allows Captive Portal phones with NO Internet connection ("Không có kết nối Internet") to watch YouTube campaign videos!
        if (cleanFilename.startsWith("yt_") && cleanFilename.endsWith(".mp4") && cleanFilename.length() > 7) {
            String ytId = cleanFilename.substring(3, cleanFilename.length() - 4);
            ResponseEntity<StreamingResponseBody> ytStream = streamAndCacheYouTubeVideo(ytId, cleanCategory, cleanFilename, rangeHeader);
            if (ytStream != null) {
                return ytStream;
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/api/portal/media/yt-video/{videoId}")
    public ResponseEntity<StreamingResponseBody> streamYouTubeVideoById(
            @PathVariable("videoId") String videoId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {
        String cleanId = videoId != null ? videoId.replaceAll("[^a-zA-Z0-9_-]", "") : "";
        if (cleanId.length() < 6 || cleanId.length() > 20) {
            return ResponseEntity.badRequest().build();
        }
        return streamMedia("campaign_videos", "yt_" + cleanId + ".mp4", rangeHeader);
    }

    /**
     * Proactively pre-downloads and caches a YouTube campaign video into Local Disk and MinIO
     * whenever a campaign is created, updated, or approved so Captive Portal guests can stream
     * it immediately as a same-origin MP4 even without Internet access.
     */
    public void warmUpYouTubeVideoAsync(String rawVideoUrl) {
        String videoId = extractYouTubeId(rawVideoUrl);
        if (videoId == null) return;
        String cleanFilename = "yt_" + videoId + ".mp4";
        Path localPath = Paths.get(baseUploadDir, "campaign_videos", cleanFilename).toAbsolutePath().normalize();
        if (Files.exists(localPath)) {
            return;
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String directUrl = resolveYouTubeDirectMp4Url(videoId);
                if (StringUtils.hasText(directUrl)) {
                    triggerAsyncYouTubeCacheDownload(videoId, directUrl, "campaign_videos", cleanFilename);
                }
            } catch (Exception e) {
                log.debug("Failed to warm up YouTube video {}: {}", videoId, e.getMessage());
            }
        });
    }

    public static String extractYouTubeId(String url) {
        if (!StringUtils.hasText(url)) return null;
        String trimmed = url.trim();
        java.util.regex.Matcher m1 = java.util.regex.Pattern
                .compile("(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:embed/|v/|shorts/|live/))([a-zA-Z0-9_-]{11})")
                .matcher(trimmed);
        if (m1.find()) return m1.group(1);
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("[?&]v=([a-zA-Z0-9_-]{11})")
                .matcher(trimmed);
        if (m2.find()) return m2.group(1);
        return null;
    }

    private ResponseEntity<StreamingResponseBody> streamAndCacheYouTubeVideo(
            String videoId,
            String cleanCategory,
            String cleanFilename,
            String rangeHeader
    ) {
        String cleanId = videoId.replaceAll("[^a-zA-Z0-9_-]", "");
        if (cleanId.length() < 6 || cleanId.length() > 20) return null;

        String directMp4Url = resolveYouTubeDirectMp4Url(cleanId);
        if (!StringUtils.hasText(directMp4Url)) {
            return null;
        }

        // Trigger async background download into Local Disk + MinIO so subsequent viewers stream from local cache in <2ms
        triggerAsyncYouTubeCacheDownload(cleanId, directMp4Url, cleanCategory, cleanFilename);

        try {
            return streamFromRemoteUrl(directMp4Url, rangeHeader, "video/mp4");
        } catch (Exception e) {
            log.warn("Failed to proxy live YouTube MP4 stream for {}: {}", cleanId, e.getMessage());
            return null;
        }
    }

    private String resolveYouTubeDirectMp4Url(String videoId) {
        if (redisTemplate != null) {
            try {
                String cachedUrl = redisTemplate.opsForValue().get("yt:stream:url:" + videoId);
                if (cachedUrl != null && !cachedUrl.isBlank()) {
                    return cachedUrl;
                }
            } catch (Exception ignored) {
            }
        }

        CachedStreamUrl cached = ytStreamUrlCache.get(videoId);
        if (cached != null && !cached.isExpired()) {
            return cached.url;
        }

        String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
        String[][] commands = new String[][]{
                {"yt-dlp", "--js-runtimes", "node", "--remote-components", "ejs:github", "-f", "18/best[ext=mp4]", "-g", watchUrl},
                {"python", "-m", "yt_dlp", "--js-runtimes", "node", "--remote-components", "ejs:github", "-f", "18/best[ext=mp4]", "-g", watchUrl},
                {"python3", "-m", "yt_dlp", "--js-runtimes", "node", "--remote-components", "ejs:github", "-f", "18/best[ext=mp4]", "-g", watchUrl},
                {"yt-dlp", "-f", "18/best[ext=mp4]", "-g", watchUrl}
        };

        for (String[] cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(false);
                Process proc = pb.start();
                String stdout;
                try (InputStream is = proc.getInputStream()) {
                    stdout = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                }
                boolean finished = proc.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    continue;
                }
                if (proc.exitValue() == 0 && stdout.startsWith("http")) {
                    String firstLine = stdout.split("\\R")[0].trim();
                    if (firstLine.startsWith("http")) {
                        ytStreamUrlCache.put(videoId, new CachedStreamUrl(firstLine, Instant.now().plus(Duration.ofHours(2))));
                        if (redisTemplate != null) {
                            try {
                                redisTemplate.opsForValue().set("yt:stream:url:" + videoId, firstLine, 2, java.util.concurrent.TimeUnit.HOURS);
                            } catch (Exception ignored) {
                            }
                        }
                        log.info("[MediaStream] Resolved direct MP4 stream for YouTube video {}", videoId);
                        return firstLine;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void triggerAsyncYouTubeCacheDownload(String videoId, String directMp4Url, String cleanCategory, String cleanFilename) {
        if (!activeYtDownloads.add(videoId)) {
            return;
        }
        if (redisTemplate != null) {
            try {
                Boolean locked = redisTemplate.opsForValue().setIfAbsent("yt:download:lock:" + videoId, "1", Duration.ofMinutes(10));
                if (Boolean.FALSE.equals(locked)) {
                    activeYtDownloads.remove(videoId);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Path targetDir = Paths.get(baseUploadDir, cleanCategory).toAbsolutePath().normalize();
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                Path finalFile = targetDir.resolve(cleanFilename);
                if (Files.exists(finalFile) && Files.size(finalFile) > 1024) {
                    return;
                }
                Path tempFile = targetDir.resolve(cleanFilename + ".part");
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(directMp4Url))
                        .timeout(Duration.ofMinutes(3))
                        .GET()
                        .build();
                HttpResponse<Path> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(tempFile));
                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && Files.exists(tempFile) && Files.size(tempFile) > 1024) {
                    Files.move(tempFile, finalFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log.info("[MediaStream] Cached YouTube video {} to local disk: {} ({} bytes)", videoId, finalFile, Files.size(finalFile));

                    if (minioClient != null && minioProperties != null && minioProperties.isEnabled()) {
                        String bucket = minioProperties.getBucket();
                        String objectKey = cleanCategory + "/" + cleanFilename;
                        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                        if (!found) {
                            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                        }
                        try (InputStream fis = Files.newInputStream(finalFile)) {
                            minioClient.putObject(
                                    PutObjectArgs.builder()
                                            .bucket(bucket)
                                            .object(objectKey)
                                            .stream(fis, Files.size(finalFile), -1)
                                            .contentType("video/mp4")
                                            .build()
                            );
                            log.info("[MediaStream] Uploaded cached YouTube video {} to MinIO: {}/{}", videoId, bucket, objectKey);
                        }
                    }
                } else {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                log.debug("Background YouTube cache download failed for {}: {}", videoId, e.getMessage());
            } finally {
                activeYtDownloads.remove(videoId);
                if (redisTemplate != null) {
                    try {
                        redisTemplate.delete("yt:download:lock:" + videoId);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    /**
     * Same-origin proxy for YouTube video thumbnails so phones in a pre-auth Captive Portal Walled Garden
     * never show a black poster when youtube.com / i.ytimg.com is blocked by the router firewall.
     */
    @GetMapping("/api/portal/media/yt-thumb/{videoId}")
    public ResponseEntity<byte[]> proxyYouTubeThumbnail(@PathVariable("videoId") String videoId) {
        String cleanId = videoId != null ? videoId.replaceAll("[^a-zA-Z0-9_-]", "") : "";
        if (cleanId.length() < 6 || cleanId.length() > 20) {
            return ResponseEntity.badRequest().build();
        }

        CachedBinary cached = ytThumbCache.get(cleanId);
        if (cached != null && !cached.isExpired()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, cached.contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(cached.data);
        }

        String thumbUrl = "https://i.ytimg.com/vi/" + cleanId + "/hqdefault.jpg";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(thumbUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null && resp.body().length > 0) {
                String ct = resp.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse("image/jpeg");
                ytThumbCache.put(cleanId, new CachedBinary(resp.body(), ct, Instant.now().plus(Duration.ofHours(6))));
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, ct)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .body(resp.body());
            }
        } catch (Exception e) {
            log.debug("Could not fetch YouTube thumbnail for {}: {}", cleanId, e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/banners/haiphong_city_bg.jpg")
                .build();
    }

    private ResponseEntity<StreamingResponseBody> streamFromLocalFile(
            Path localPath,
            String rangeHeader,
            String contentType
    ) throws IOException {
        long fileSize = Files.size(localPath);
        if (fileSize <= 0) {
            return ResponseEntity.noContent().build();
        }

        long[] range = parseRange(rangeHeader, fileSize);
        boolean isPartial = range != null;
        long start = isPartial ? range[0] : 0;
        long end = isPartial ? range[1] : (fileSize - 1);
        long contentLength = end - start + 1;

        StreamingResponseBody body = outputStream -> {
            try (RandomAccessFile raf = new RandomAccessFile(localPath.toFile(), "r")) {
                raf.seek(start);
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = contentLength;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = raf.read(buffer, 0, toRead);
                    if (read == -1) break;
                    outputStream.write(buffer, 0, read);
                    remaining -= read;
                }
                outputStream.flush();
            }
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(isPartial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800");

        if (isPartial) {
            builder.header(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize));
        }

        return builder.body(body);
    }

    private ResponseEntity<StreamingResponseBody> streamFromMinio(
            String objectKey,
            String rangeHeader,
            String defaultMime
    ) throws Exception {
        String bucket = minioProperties.getBucket();
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
        );
        long fileSize = stat.size();
        String contentType = StringUtils.hasText(stat.contentType()) && !"application/octet-stream".equalsIgnoreCase(stat.contentType())
                ? stat.contentType()
                : defaultMime;

        long[] range = parseRange(rangeHeader, fileSize);
        boolean isPartial = range != null;
        long start = isPartial ? range[0] : 0;
        long end = isPartial ? range[1] : (fileSize - 1);
        long contentLength = end - start + 1;

        StreamingResponseBody body = outputStream -> {
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .offset(start)
                            .length(contentLength)
                            .build()
            )) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = contentLength;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = is.read(buffer, 0, toRead);
                    if (read == -1) break;
                    outputStream.write(buffer, 0, read);
                    remaining -= read;
                }
                outputStream.flush();
            } catch (Exception e) {
                throw new IOException("Error streaming object from MinIO: " + objectKey, e);
            }
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(isPartial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800");

        if (isPartial) {
            builder.header(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize));
        }

        return builder.body(body);
    }

    private ResponseEntity<StreamingResponseBody> streamFromRemoteUrl(
            String remoteUrl,
            String rangeHeader,
            String defaultMime
    ) throws Exception {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(remoteUrl))
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (StringUtils.hasText(rangeHeader)) {
            reqBuilder.header(HttpHeaders.RANGE, rangeHeader);
        }

        HttpResponse<InputStream> remoteResp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        int status = remoteResp.statusCode();
        if (status != 200 && status != 206) {
            remoteResp.body().close();
            return ResponseEntity.status(status).build();
        }

        String contentType = remoteResp.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(defaultMime);
        if ("application/octet-stream".equalsIgnoreCase(contentType)) {
            contentType = defaultMime;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800");

        remoteResp.headers().firstValue(HttpHeaders.CONTENT_LENGTH)
                .ifPresent(len -> builder.header(HttpHeaders.CONTENT_LENGTH, len));
        remoteResp.headers().firstValue(HttpHeaders.CONTENT_RANGE)
                .ifPresent(cr -> builder.header(HttpHeaders.CONTENT_RANGE, cr));

        StreamingResponseBody body = outputStream -> {
            try (InputStream is = remoteResp.body()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        };

        return builder.body(body);
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        if (!StringUtils.hasText(rangeHeader) || !rangeHeader.startsWith("bytes=") || fileSize <= 0) {
            return null;
        }
        try {
            String rangeSpec = rangeHeader.substring("bytes=".length()).trim();
            int commaIdx = rangeSpec.indexOf(',');
            if (commaIdx > 0) {
                rangeSpec = rangeSpec.substring(0, commaIdx).trim();
            }
            int dashIdx = rangeSpec.indexOf('-');
            if (dashIdx < 0) return null;

            String startStr = rangeSpec.substring(0, dashIdx).trim();
            String endStr = rangeSpec.substring(dashIdx + 1).trim();

            long start;
            long end;
            if (startStr.isEmpty()) {
                // Suffix range: bytes=-500
                long suffixLen = Long.parseLong(endStr);
                start = Math.max(0, fileSize - suffixLen);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(startStr);
                if (endStr.isEmpty()) {
                    // Open-ended range: bytes=0- -> cap chunk for responsive mobile video playback
                    end = Math.min(fileSize - 1, start + MAX_CHUNK_BYTES - 1);
                } else {
                    end = Math.min(fileSize - 1, Long.parseLong(endStr));
                }
            }

            if (start < 0 || start >= fileSize || end < start) {
                return null;
            }
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeSegment(String input) {
        if (!StringUtils.hasText(input)) return "";
        String cleaned = input.trim().replace("\\", "/");
        if (cleaned.contains("..") || cleaned.contains("/")) {
            return "";
        }
        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private String inferContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".ogv") || lower.endsWith(".ogg")) return "video/ogg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
