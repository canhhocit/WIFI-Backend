package com.wificaptive.upload.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.upload.dto.UploadResponseDto;
import com.wificaptive.upload.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/uploads", "/api/v1/admin/uploads"})
public class UploadController {

    private final StorageService storageService;

    @Autowired
    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponseDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "banners") String category
    ) {
        UploadResponseDto result = storageService.store(file, category);
        return ResponseEntity.ok(ApiResponse.ok("Tải tệp tin lên thành công", result));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Boolean>> deleteFile(@RequestParam("url") String fileUrl) {
        boolean deleted = storageService.delete(fileUrl);
        return ResponseEntity.ok(ApiResponse.ok("Xóa tệp tin hoàn tất", deleted));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageStatus() {
        return ResponseEntity.ok(ApiResponse.ok("Trạng thái lưu trữ", Map.of(
                "minioActive", storageService.isMinioActive(),
                "storageEngine", storageService.isMinioActive() ? "MinIO Object Storage (S3)" : "Local File System"
        )));
    }
}
