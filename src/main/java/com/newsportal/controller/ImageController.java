package com.newsportal.controller;

import com.newsportal.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFileAsResource(filename);
        MediaType mediaType = resolveMediaType(resource);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private MediaType resolveMediaType(Resource resource) {
        try {
            Path path = resource.getFile().toPath();
            String contentType = Files.probeContentType(path);
            if (contentType != null) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // fallback below
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
