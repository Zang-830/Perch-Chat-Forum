package com.campus.community.file;

import com.campus.community.common.ApiResponse;
import com.campus.community.file.dto.UploadResult;
import com.campus.community.security.CurrentUser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final ImageUploadService imageUploadService;

    public FileController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/images")
    public ApiResponse<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(imageUploadService.upload(file, CurrentUser.id()));
    }

    @GetMapping("/images/{year:\\d{4}}/{month:\\d{2}}/{filename:[a-zA-Z0-9.]+}")
    public ResponseEntity<InputStreamResource> image(@PathVariable String year,
                                                     @PathVariable String month,
                                                     @PathVariable String filename) {
        ImageUploadService.ImageDownload image = imageUploadService.open(year + "/" + month + "/" + filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(new InputStreamResource(image.stream()));
    }
}
