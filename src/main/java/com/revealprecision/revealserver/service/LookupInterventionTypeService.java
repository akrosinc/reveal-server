package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.LookupInterventionTypeRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import com.revealprecision.revealserver.persistence.domain.LookupInterventionType.Fields;
import com.revealprecision.revealserver.persistence.repository.LookupInterventionTypeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LookupInterventionTypeService {

  private final LookupInterventionTypeRepository interventionTypeRepository;

  public LookupInterventionType findByIdentifier(UUID identifier) {
    return interventionTypeRepository.findById(identifier).orElseThrow(() -> new NotFoundException(
        Pair.of(Fields.identifier, identifier), LookupInterventionType.class));
  }

  public void createInterventionType(LookupInterventionTypeRequest request) {
    if (interventionTypeRepository.findByNameOrCode(request.getName(), request.getCode())
        .isPresent()) {
      throw new ConflictException("Intervention type already exist");
    }
    LookupInterventionType interventionType = LookupInterventionType.builder()
        .name(request.getName())
        .code(request.getCode())
        .build();
    interventionType.setEntityStatus(EntityStatus.ACTIVE);
    interventionTypeRepository.save(interventionType);
  }
}
