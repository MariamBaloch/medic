package com.ga.medic.identity.service;

import com.ga.medic.identity.dto.response.UserAccountResponse;
import com.ga.medic.identity.enums.UserStatusEnum;
import com.ga.medic.identity.mapper.UserMapper;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorDirectoryService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> getActiveDoctors(String search, Long specializationId) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
        return userMapper.toResponses(doctorProfileRepository.findActiveDoctors(
                        UserStatusEnum.ACTIVE,
                        normalizedSearch,
                        specializationId
                )
                .stream()
                .map(DoctorProfile::getUser)
                .toList());
    }
}
