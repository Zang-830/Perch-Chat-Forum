package com.campus.community.file.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {
    private final Path uploadRoot;

    public LocalImageStorage(@Value("${app.upload.directory}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void store(String objectName, byte[] content, String contentType) throws Exception {
        Path target = resolve(objectName);
        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public InputStream open(String objectName) throws Exception {
        return Files.newInputStream(resolve(objectName));
    }

    @Override
    public void delete(String objectName) {
        try {
            Files.deleteIfExists(resolve(objectName));
        } catch (Exception ignored) {
            // Best-effort cleanup after a failed database write.
        }
    }

    private Path resolve(String objectName) {
        Path target = uploadRoot.resolve(objectName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid image object name");
        }
        return target;
    }
}
