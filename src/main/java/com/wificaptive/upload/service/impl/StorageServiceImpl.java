package com.wificaptive.upload.service.impl;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.upload.config.MinioProperties;
import com.wificaptive.upload.dto.UploadResponseDto;
import com.wificaptive.upload.service.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceImpl.class);

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir = "uploads";

    @Autowired
    public StorageServiceImpl(
            MinioProperties minioProperties,
            @Autowired(required = false) MinioClient minioClient
    ) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
    }

    @Override
    public boolean isMinioActive() {
        return minioClient != null && minioProperties.isEnabled();
    }

    @Override
    public UploadResponseDto store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp tin để tải lên.");
        }

        // Maximum size: 100MB
        if (file.getSize() > 100L * 1024 * 1024) {
            throw new BadRequestException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 100MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String cleanCategory = StringUtils.hasText(category)
                ? category.replaceAll("[^a-zA-Z0-9_-]", "")
                : "banners";
        if (cleanCategory.isEmpty()) cleanCategory = "banners";

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        } else {
            extension = inferExtension(contentType);
        }

        String uniqueName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        String objectKey = cleanCategory + "/" + uniqueName;

        // Try MinIO storage first if active
        if (isMinioActive()) {
            try {
                return storeInMinio(file, objectKey, uniqueName, originalName, contentType);
            } catch (Exception e) {
                log.error("Failed to store file in MinIO: {}. Falling back to local disk storage.", e.getMessage(), e);
            }
        }

        // Fallback: Local disk storage
        return storeInLocal(file, cleanCategory, uniqueName, originalName, contentType);
    }

    private UploadResponseDto storeInMinio(
            MultipartFile file,
            String objectKey,
            String uniqueName,
            String originalName,
            String contentType
    ) throws Exception {
        String bucket = minioProperties.getBucket();

        // Ensure bucket exists
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            log.info("Bucket {} does not exist in MinIO. Creating...", bucket);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        }

        // Return same-origin relative path (/uploads/...) so Captive Portal guests inside the router's
        // pre-authentication Walled Garden can load images and stream videos without external domain blocks.
        String fileUrl = "/uploads/" + objectKey;
        log.info("File successfully uploaded to MinIO: bucket={}, key={}, url={}", bucket, objectKey, fileUrl);

        return new UploadResponseDto(
                fileUrl,
                uniqueName,
                originalName,
                file.getSize(),
                contentType
        );
    }

    private UploadResponseDto storeInLocal(
            MultipartFile file,
            String cleanCategory,
            String uniqueName,
            String originalName,
            String contentType
    ) {
        try {
            Path targetDir = Paths.get(baseUploadDir, cleanCategory).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path destination = targetDir.resolve(uniqueName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + cleanCategory + "/" + uniqueName;
            log.info("File stored on local disk at {} with URL {}", destination, fileUrl);

            return new UploadResponseDto(
                    fileUrl,
                    uniqueName,
                    originalName,
                    file.getSize(),
                    contentType
            );
        } catch (IOException e) {
            log.error("Failed to save uploaded file to local disk", e);
            throw new BadRequestException("Không thể lưu trữ tệp tin đã tải lên: " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return false;

        if (isMinioActive() && fileUrl.contains(minioProperties.getBucket())) {
            try {
                String bucket = minioProperties.getBucket();
                int idx = fileUrl.indexOf(bucket);
                String objectKey = fileUrl.substring(idx + bucket.length() + 1);
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
                log.info("Deleted object from MinIO: {}", objectKey);
                return true;
            } catch (Exception e) {
                log.warn("Failed to delete object from MinIO: {}", e.getMessage());
            }
        }

        // Local deletion
        if (fileUrl.startsWith("/uploads/")) {
            try {
                String relPath = fileUrl.substring("/uploads/".length());
                Path p = Paths.get(baseUploadDir, relPath).toAbsolutePath().normalize();
                return Files.deleteIfExists(p);
            } catch (Exception e) {
                log.warn("Failed to delete local file: {}", e.getMessage());
            }
        }
        return false;
    }

    private String inferExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            case "application/pdf" -> ".pdf";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            case "video/x-matroska" -> ".mkv";
            case "video/ogg" -> ".ogv";
            default -> ".bin";
        };
    }
}
