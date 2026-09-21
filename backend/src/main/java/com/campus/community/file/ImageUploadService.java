package com.campus.community.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.common.BizException;
import com.campus.community.file.dto.UploadResult;
import com.campus.community.file.entity.UploadedFile;
import com.campus.community.file.mapper.UploadedFileMapper;
import com.campus.community.file.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageUploadService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif"
    );

    private final UploadedFileMapper fileMapper;
    private final ImageStorage imageStorage;
    private final long maxImageBytes;

    public ImageUploadService(UploadedFileMapper fileMapper,
                              ImageStorage imageStorage,
                              @Value("${app.upload.max-image-bytes}") long maxImageBytes) {
        this.fileMapper = fileMapper;
        this.imageStorage = imageStorage;
        this.maxImageBytes = maxImageBytes;
    }

    public UploadResult upload(MultipartFile multipartFile, Long userId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (multipartFile.getSize() > maxImageBytes) {
            throw new BizException("单张图片不能超过 5MB");
        }

        String contentType = multipartFile.getContentType() == null
                ? "" : multipartFile.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BizException("仅支持 JPG、PNG 和 GIF 图片");
        }

        String storageName = null;
        try {
            byte[] content = multipartFile.getBytes();
            if (!contentType.equals(detectContentType(content))) {
                throw new BizException("图片格式与文件内容不一致");
            }
            int[] dimensions = readDimensions(content);
            if (dimensions[0] <= 0 || dimensions[1] <= 0) {
                throw new BizException("图片内容无法识别");
            }
            if (dimensions[0] > 10000 || dimensions[1] > 10000) {
                throw new BizException("图片尺寸过大");
            }

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            storageName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
            imageStorage.store(storageName, content, contentType);

            UploadedFile file = new UploadedFile();
            file.setUserId(userId);
            file.setOriginalName(safeOriginalName(multipartFile.getOriginalFilename()));
            file.setStorageName(storageName);
            file.setContentType(contentType);
            file.setFileSize((long) content.length);
            file.setPublicUrl("/api/files/images/" + storageName);
            file.setStatus("ACTIVE");
            file.setCreatedAt(LocalDateTime.now());
            try {
                fileMapper.insert(file);
            } catch (RuntimeException exception) {
                imageStorage.delete(storageName);
                throw exception;
            }
            return new UploadResult(file.getId(), file.getPublicUrl(), file.getOriginalName(), contentType, content.length);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            if (storageName != null) {
                imageStorage.delete(storageName);
            }
            throw new BizException(503, "图片保存失败，请稍后再试");
        }
    }

    public ImageDownload open(String storageName) {
        UploadedFile file = fileMapper.selectOne(Wrappers.<UploadedFile>lambdaQuery()
                .eq(UploadedFile::getStorageName, storageName)
                .eq(UploadedFile::getStatus, "ACTIVE"));
        if (file == null) {
            throw new BizException(404, "图片不存在");
        }
        try {
            return new ImageDownload(imageStorage.open(storageName), file.getContentType(), file.getFileSize());
        } catch (Exception exception) {
            throw new BizException(503, "图片暂时无法读取，请稍后再试");
        }
    }

    public List<UploadedFile> requireOwnedFiles(List<String> urls, Long userId) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        List<String> normalized = urls.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (normalized.size() > 9 || new HashSet<>(normalized).size() != normalized.size()) {
            throw new BizException("每篇帖子最多上传 9 张不重复的图片");
        }
        List<UploadedFile> files = fileMapper.selectList(Wrappers.<UploadedFile>lambdaQuery()
                .eq(UploadedFile::getUserId, userId)
                .eq(UploadedFile::getStatus, "ACTIVE")
                .in(UploadedFile::getPublicUrl, normalized));
        if (files.size() != normalized.size()) {
            throw new BizException("部分图片不存在或不属于当前账号");
        }
        Map<String, UploadedFile> byUrl = files.stream()
                .collect(java.util.stream.Collectors.toMap(UploadedFile::getPublicUrl, file -> file));
        List<UploadedFile> ordered = new ArrayList<>();
        normalized.forEach(url -> ordered.add(byUrl.get(url)));
        return ordered;
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        String normalized = originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (normalized.isBlank()) {
            normalized = "image";
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }

    private String detectContentType(byte[] content) {
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (content.length >= 8
                && (content[0] & 0xff) == 0x89
                && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47
                && content[4] == 0x0d && content[5] == 0x0a && content[6] == 0x1a && content[7] == 0x0a) {
            return "image/png";
        }
        if (content.length >= 6) {
            String signature = new String(content, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) {
                return "image/gif";
            }
        }
        return "";
    }

    private int[] readDimensions(byte[] content) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new BizException("图片内容无法识别");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BizException("图片内容无法识别");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    public record ImageDownload(InputStream stream, String contentType, long size) {
    }
}
