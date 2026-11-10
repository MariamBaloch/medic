package com.ga.medic.identity.service;

import com.ga.medic.identity.dto.response.SpecializationResponse;
import com.ga.medic.identity.mapper.SpecializationMapper;
import com.ga.medic.identity.model.Specialization;
import com.ga.medic.identity.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final SpecializationMapper specializationMapper;

    public List<SpecializationResponse> getSpecializations(String search) {
        List<Specialization> specializations = search == null || search.isBlank()
                ? specializationRepository.findAllByOrderByNameAsc()
                : specializationRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByNameAsc(
                        search.trim(), search.trim()
                );

        return specializationMapper.toResponses(specializations);
    }
}
