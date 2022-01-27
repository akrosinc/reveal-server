package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.FormEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.FormRequest;
import com.revealprecision.revealserver.api.v1.dto.request.FormUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.FlagEnum;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Form.Fields;
import com.revealprecision.revealserver.persistence.repository.FormRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FormService {

  private final FormRepository formRepository;

  public void createFrom(FormRequest formRequest) {
    if (formRepository.findByName(formRequest.getName()).isPresent()) {
      throw new ConflictException(String.format(
          Error.NON_UNIQUE, Fields.name, formRequest.getName()));
    }
    Form form = FormEntityFactory.toEntity(formRequest);
    form.setEntityStatus(EntityStatus.ACTIVE);
    formRepository.save(form);
  }

  public Form findById(UUID identifier) {
    return formRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of(Fields.identifier, identifier), Form.class));
  }

  public Page<Form> getAll(String search, Pageable pageable, FlagEnum template) {
    if (template != null) {
      return formRepository.findAllByCriteriaAndTemplate(pageable, search,
          template == FlagEnum.TRUE ? true : false);
    } else {
      return formRepository.findAllByCriteria(pageable, search);
    }
  }

  public void updateForm(FormUpdateRequest updateRequest, UUID identifier) {
    Form form = findById(identifier);
    form.setPayload(updateRequest.getPayload());
    form.setTitle(updateRequest.getTitle());
    formRepository.save(form);
  }

  public void deleteForm(UUID identifier) {
    Form form = findById(identifier);
    formRepository.delete(form);
  }

  public Map<UUID, Form> findByIdentifiers(Set<UUID> identifiers) {
    List<UUID> findForms = new ArrayList<>(identifiers);

    List<Form> foundForms = formRepository.getFormsByIdentifiers(findForms);
    identifiers.removeAll(
        foundForms
            .stream()
            .map(Form::getIdentifier)
            .collect(Collectors.toList()));
    if (identifiers.size() > 0) {
      throw new ConflictException("Forms: " + identifiers + " does not exist");
    } else {
      Map<UUID, Form> response = new HashMap<>();
      MapUtils.populateMap(response, foundForms, Form::getIdentifier);
      return response;
    }
  }
}
