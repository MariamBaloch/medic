package com.ga.medic.service;

import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.mapper.SpecializationMapper;
import com.ga.medic.model.Specialization;
import com.ga.medic.repository.SpecializationRepository;
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
