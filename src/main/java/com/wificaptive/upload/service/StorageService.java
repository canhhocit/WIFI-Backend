package com.wificaptive.upload.service;

import com.wificaptive.upload.dto.UploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Tải lên và lưu trữ tệp (hình ảnh, tài liệu số, media) vào hệ thống lưu trữ (MinIO hoặc Local).
     *
     * @param file Tệp tin cần tải lên
     * @param category Thư mục phân loại (banners, media, documents,...)
     * @return Thông tin tệp đã lưu kèm URL truy cập trực tiếp
     */
    UploadResponseDto store(MultipartFile file, String category);

    /**
     * Xóa tệp khỏi hệ thống lưu trữ.
     *
     * @param fileUrl URL của tệp cần xóa
     * @return true nếu xóa thành công
     */
    boolean delete(String fileUrl);

    /**
     * Kiểm tra MinIO có đang được kích hoạt và hoạt động hay không.
     */
    boolean isMinioActive();
}
