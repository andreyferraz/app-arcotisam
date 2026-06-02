package com.arcotisam.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve absolute path to avoid relative resolution issues in different runtimes
        String absolute = Paths.get(uploadDir).toAbsolutePath().toString();
        if (!absolute.endsWith("/")) {
            absolute = absolute + "/";
        }
        // Expose filesystem upload directory under /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute);
    }
}
