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

    /**
     * Stores a non-empty file under a generated name while retaining its original extension.
     *
     * @param file the file to store
     * @param subFolder the folder under the configured upload directory
     * @return the stored file's path
     */
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

    /**
     * Deletes a stored file when present; filesystem deletion errors are ignored.
     *
     * @param filePath the path of the file to delete
     */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path file = Paths.get(filePath.replaceFirst("^/", "")).toAbsolutePath().normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {

        }
    }

    /**
     * Extracts a lowercase extension from the uploaded file's original name.
     *
     * @param file the uploaded file whose original name is inspected
     * @return the lowercase file extension, including its leading dot
     */
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename != null && originalFilename.contains(".") && !originalFilename.endsWith(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        throw new IllegalArgumentException("Invalid file name. File must have a valid extension (e.g., .jpg, .png)");
    }
}
