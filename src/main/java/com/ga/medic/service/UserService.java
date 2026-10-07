package com.ga.medic.service;

import com.ga.medic.config.Constants;
import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.User;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService storageService;
    private final AuthenticatedUser authenticatedUser;
    private final UserMapper userMapper;

    /**
     * Validates and stores a replacement avatar, then updates the authenticated user's image URL.
     *
     * @param file the image file to use as the new avatar
     * @return the stored avatar path
     */
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

    /**
     * Retrieves the authenticated user's account details.
     *
     * @return the authenticated user's account response
     */
    @Transactional
    public UserAccountResponse getUserAccount() {
        Long userId = authenticatedUser.getUserId();
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));
        return userMapper.toResponse(user);
    }

    /**
     * Marks a user as deleted while retaining the account and its related records.
     *
     * @param userId the ID of the account to soft delete
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));

        user.setStatus(UserStatusEnum.DELETED);
        if (user.getDeletedAt() == null) {
            user.setDeletedAt(Instant.now());
        }
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
    }
}
