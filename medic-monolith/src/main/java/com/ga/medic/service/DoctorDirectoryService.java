package com.ga.medic.service;

import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorDirectoryService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> getActiveDoctors(String search, Long specializationId, Boolean hasFutureAvailability) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
        return userMapper.toResponses(doctorProfileRepository.findActiveDoctors(
                        UserStatusEnum.ACTIVE,
                        normalizedSearch,
                        specializationId,
                        hasFutureAvailability,
                        LocalDate.now()
                )
                .stream()
                .map(DoctorProfile::getUser)
                .toList());
    }
}
