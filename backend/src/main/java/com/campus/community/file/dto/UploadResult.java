package com.campus.community.file.dto;

public record UploadResult(Long id, String url, String originalName, String contentType, long size) {
}

