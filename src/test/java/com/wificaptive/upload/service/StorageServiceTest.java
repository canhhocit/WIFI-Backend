package com.wificaptive.upload.service;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.upload.config.MinioProperties;
import com.wificaptive.upload.dto.UploadResponseDto;
import com.wificaptive.upload.service.impl.StorageServiceImpl;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private MinioProperties minioProperties;
    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        minioProperties.setEnabled(false); // test local fallback
        storageService = new StorageServiceImpl(minioProperties, null);
    }

    @Test
    @DisplayName("Lưu trữ tệp cục bộ khi MinIO không kích hoạt")
    void store_LocalFallback() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-banner.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        UploadResponseDto response = storageService.store(file, "banners");

        assertNotNull(response);
        assertNotNull(response.getUrl());
        assertTrue(response.getUrl().startsWith("/uploads/banners/"));
        assertEquals("test-banner.jpg", response.getOriginalName());
        assertEquals("image/jpeg", response.getContentType());
        assertTrue(response.getFileSize() > 0);

        // Clean up created file
        storageService.delete(response.getUrl());
    }

    @Test
    @DisplayName("Từ chối tệp tin rỗng")
    void store_RejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(BadRequestException.class, () -> storageService.store(emptyFile, "banners"));
    }

    @Test
    @DisplayName("Xóa tệp cục bộ thành công")
    void delete_LocalFile() {
        MockMultipartFile file = new MockMultipartFile("file", "to-delete.png", "image/png", "sample data".getBytes());
        UploadResponseDto response = storageService.store(file, "test-cat");

        boolean deleted = storageService.delete(response.getUrl());
        assertTrue(deleted);
    }

    @Test
    @DisplayName("Tải lên và lưu trữ trực tiếp vào MinIO S3 khi kích hoạt")
    void store_MinioLiveTest() {
        MinioProperties liveMinioProps = new MinioProperties();
        liveMinioProps.setEnabled(true);
        liveMinioProps.setEndpoint("https://minio.phuchoangmai.xyz");
        liveMinioProps.setAccessKey("17PESIFWAOM0EZKV6L5P");
        liveMinioProps.setSecretKey("6mtgAxn+PBXUDQZZ7XemXiGLk6CwOkWzCowZXbRA");
        liveMinioProps.setBucket("wifi-haiphong");
        liveMinioProps.setPublicUrl("https://minio.phuchoangmai.xyz/wifi-haiphong");

        MinioClient liveClient = MinioClient.builder()
                .endpoint(liveMinioProps.getEndpoint())
                .credentials(liveMinioProps.getAccessKey(), liveMinioProps.getSecretKey())
                .build();

        StorageServiceImpl liveStorageService = new StorageServiceImpl(liveMinioProps, liveClient);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "minio-test-upload.jpg",
                "image/jpeg",
                "test binary data for minio".getBytes()
        );

        UploadResponseDto response = liveStorageService.store(file, "banners");

        assertNotNull(response);
        assertNotNull(response.getUrl());
        assertTrue(response.getUrl().startsWith("https://minio.phuchoangmai.xyz/wifi-haiphong/banners/"));
        assertEquals("minio-test-upload.jpg", response.getOriginalName());
        assertEquals("image/jpeg", response.getContentType());

        // Verify deletion from MinIO
        boolean deleted = liveStorageService.delete(response.getUrl());
        assertTrue(deleted);
    }
}
