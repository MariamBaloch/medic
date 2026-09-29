package com.ga.medic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${app.upload.base-dir:uploads}")
    private String baseDirectory;

    public String store(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        try {
            Path targetDirectory = Paths.get(baseDirectory, subFolder).toAbsolutePath().normalize();
            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            String extension = getFileExtension(file);
            String storedFileName = UUID.randomUUID() + extension;

            Path targetLocation = targetDirectory.resolve(storedFileName);
            file.transferTo(targetLocation);

            return "/" + baseDirectory + "/" + subFolder + "/" + storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file", ex);
        }
    }

    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path file = Paths.get(filePath.replaceFirst("^/", "")).toAbsolutePath().normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {

        }
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename != null && originalFilename.contains(".") && !originalFilename.endsWith(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        throw new IllegalArgumentException("Invalid file name. File must have a valid extension (e.g., .jpg, .png)");
    }
}