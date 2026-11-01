package com.wificaptive.upload.dto;

public class UploadResponseDto {
    private String url;
    private String fileName;
    private String originalName;
    private long fileSize;
    private String contentType;

    public UploadResponseDto() {
    }

    public UploadResponseDto(String url, String fileName, String originalName, long fileSize, String contentType) {
        this.url = url;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
