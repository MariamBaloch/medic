package com.ga.medic.identity.service;

import org.mapstruct.Named;
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
    private static final String PUBLIC_UPLOADS_SEGMENT = "/uploads/";

    @Value("${app.upload.base-dir:uploads}")
    private String baseDirectory;

    @Value("${app.public-base-path:/api/identity}")
    private String publicBasePath;

    /**
     * Stores a non-empty file under a generated name while retaining its original extension.
     *
     * @param file the file to store
     * @param subFolder the folder under the configured upload directory
     * @return gateway-facing path under the public uploads prefix
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

            return publicUploadsPrefix() + subFolder + "/" + storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file", ex);
        }
    }


    @Named("publicImageUrl")
    public String publicImageUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        String objectPath = extractObjectPath(stored);
        if (objectPath == null) {
            return stored;
        }
        return publicUploadsPrefix() + objectPath;
    }

    /**
     * Deletes a stored file when present; filesystem deletion errors are ignored.
     *
     * @param filePath the path of the file to delete (may include the public base path)
     */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        String objectPath = extractObjectPath(filePath);
        if (objectPath == null) return;
        try {
            Path base = Paths.get(baseDirectory).toAbsolutePath().normalize();
            Path file = base.resolve(objectPath).normalize();
            if (!file.startsWith(base)) return;
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

    private String publicUploadsPrefix() {
        String base = publicBasePath == null ? "" : publicBasePath.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + PUBLIC_UPLOADS_SEGMENT;
    }

    /**
     * Returns the path under the uploads directory, such as {@code avatars/file.png}.
     */
    private String extractObjectPath(String stored) {
        String path = stored.trim().replace('\\', '/').replaceAll("/{2,}", "/");
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }

        int uploadsIndex = path.toLowerCase().lastIndexOf(PUBLIC_UPLOADS_SEGMENT);
        String objectPath = uploadsIndex >= 0
                ? path.substring(uploadsIndex + PUBLIC_UPLOADS_SEGMENT.length())
                : trailingObjectPath(path);

        if (objectPath == null) {
            return null;
        }
        while (objectPath.startsWith("/")) {
            objectPath = objectPath.substring(1);
        }
        if (objectPath.isBlank() || objectPath.contains("..")) {
            return null;
        }
        return objectPath;
    }

    private String trailingObjectPath(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int fileSlash = trimmed.lastIndexOf('/');
        if (fileSlash <= 0) {
            return null;
        }
        int folderSlash = trimmed.lastIndexOf('/', fileSlash - 1);
        return folderSlash >= 0 ? trimmed.substring(folderSlash + 1) : trimmed;
    }
}