package com.wificaptive.upload.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.upload.dto.UploadResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping({"/api/admin/uploads", "/api/v1/admin/uploads"})
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponseDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "banners") String category
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp hình ảnh để tải lên.");
        }

        // Limit size: 15MB
        if (file.getSize() > 15 * 1024 * 1024) {
            throw new BadRequestException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 15MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/octet-stream"))) {
            throw new BadRequestException("Chỉ chấp nhận tệp định dạng hình ảnh (JPG, PNG, WebP, GIF, SVG).");
        }

        // Sanitize category folder
        String cleanCategory = StringUtils.hasText(category)
                ? category.replaceAll("[^a-zA-Z0-9_-]", "")
                : "banners";
        if (cleanCategory.isEmpty()) cleanCategory = "banners";

        try {
            Path targetDir = Paths.get(baseUploadDir, cleanCategory).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalName = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }

            String uniqueName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path destination = targetDir.resolve(uniqueName);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + cleanCategory + "/" + uniqueName;
            log.info("Uploaded file stored at {} with URL {}", destination, fileUrl);

            UploadResponseDto result = new UploadResponseDto(
                    fileUrl,
                    uniqueName,
                    originalName,
                    file.getSize(),
                    contentType
            );

            return ResponseEntity.ok(ApiResponse.ok("Tải hình ảnh lên thành công", result));
        } catch (IOException e) {
            log.error("Failed to save uploaded file", e);
            throw new BadRequestException("Không thể lưu trữ tệp tin đã tải lên: " + e.getMessage());
        }
    }
}
