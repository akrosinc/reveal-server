package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.CoreField;
import com.revealprecision.revealserver.persistence.domain.CoreField.Fields;
import com.revealprecision.revealserver.persistence.repository.CoreFieldRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CoreFieldService {

  private final CoreFieldRepository coreFieldRepository;

  public CoreField getCoreFieldByIdentifier(UUID identifier) {
    return coreFieldRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, identifier), CoreField.class));
  }

}
