package com.newsportal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.newsportal.exception.BadRequestException;
import com.newsportal.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path fileStorageLocation;
    private Cloudinary cloudinary;

    @Value("${CLOUDINARY_URL:#{null}}")
    private String cloudinaryUrl;

    public FileStorageService(@Value("${file.upload-dir:uploads/images}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new BadRequestException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostConstruct
    public void initCloudinary() {
        if (cloudinaryUrl != null && !cloudinaryUrl.trim().isEmpty()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                throw new BadRequestException("Uploaded file is empty");
            }
            if (originalFileName.contains("..")) {
                throw new BadRequestException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            String lowerName = originalFileName.toLowerCase(Locale.ROOT);
            String fileExtension = "";
            if (lowerName.contains(".")) {
                fileExtension = lowerName.substring(lowerName.lastIndexOf("."));
            }
            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new BadRequestException("Only JPG, JPEG, PNG and WEBP are allowed");
            }

            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new BadRequestException("Invalid content type for image upload");
            }

            if (this.cloudinary != null) {
                // Upload to Cloudinary
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                return uploadResult.get("secure_url").toString();
            } else {
                // Local fallback
                String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                return uniqueFileName;
            }
        } catch (IOException ex) {
            throw new BadRequestException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            throw new BadRequestException("Cannot load remote URL as local resource");
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("Invalid file path");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found " + fileName, ex);
        }
    }
}
