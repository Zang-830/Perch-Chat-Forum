package com.campus.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {
    private final Path uploadRoot;

    public WebMvcConfig(@Value("${app.upload.directory}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = uploadRoot.toUri().toString();
        if (!uploadLocation.endsWith("/")) {
            uploadLocation += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(86400);
    }
}
