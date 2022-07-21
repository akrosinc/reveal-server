package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.repository.LookupEntityTypeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LookupEntityTypeService {

  private final LookupEntityTypeRepository lookupEntityTypeRepository;

  public List<LookupEntityType> getAllLookUpEntityTypes() {
    return lookupEntityTypeRepository.findAll();
  }

  public LookupEntityType getLookUpEntityTypeById(UUID lookupEntityTypeIdentifier) {
    return lookupEntityTypeRepository.findById(lookupEntityTypeIdentifier).orElseThrow(
        () -> new NotFoundException(
            Pair.of(LookupEntityType.Fields.identifier, lookupEntityTypeIdentifier),
            LookupEntityType.class));
  }

  public LookupEntityType getLookupEntityTypeByTableName(String name) {
    return lookupEntityTypeRepository.findLookupEntityTypeByTableName(name).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupEntityType.Fields.tableName, name),
            LookupEntityType.class));
  }

  public LookupEntityType getLookupEntityTypeByCode(String code) {
    return lookupEntityTypeRepository.findLookupEntityTypeByCode(code).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupEntityType.Fields.code, code),
            LookupEntityType.class));
  }

}
