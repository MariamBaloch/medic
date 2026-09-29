package com.ga.medic.service;

import com.ga.medic.config.Constants;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.model.User;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;
    private final FileStorageService storageService;
    private final AuthenticatedUser authenticatedUser;


    @Transactional
    public String updateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No Image file selected");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/") || !Constants.ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only image files (JPEG, JPG, PNG, WEBP) are allowed for profile pictures.");
        }

        Long userId = authenticatedUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));

        if (user.getImageUrl() != null) {
            storageService.delete(user.getImageUrl());
        }

        String imageUrl = storageService.store(file, "avatars");

        user.setImageUrl(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }
}