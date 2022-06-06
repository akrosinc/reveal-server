package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.dto.request.SettingRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Setting;
import com.revealprecision.revealserver.persistence.domain.Setting.Fields;
import com.revealprecision.revealserver.persistence.repository.SettingRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SettingService {

  private final SettingRepository settingRepository;

  public Setting createSetting(SettingRequest request) {
    checkDuplicateKey(request.getKey());
    Setting setting = Setting.builder().key(request.getKey()).value(request.getValue())
        .values(request.getValues()).label(request.getLabel()).description(request.getDescription())
        .settingIdentifier(
            request.getSettingIdentifier()).type(request.getType()).build();
    setting.setEntityStatus(EntityStatus.ACTIVE);
    return settingRepository.save(setting);
  }

  public Setting updateSetting(UUID identifier, SettingRequest request) {
    Setting setting = findExistingByIdentifier(identifier);
    setting = setting.update(request);
    return settingRepository.save(setting);
  }

  public List<Setting> findExistingSettingsByTypeIdentifier(String settingTypeIdentifier) {
    return settingRepository.findBySettingIdentifier(settingTypeIdentifier);
  }


  private void checkDuplicateKey(String key) {
    settingRepository.findByKey(key).ifPresent(setting -> {
      throw new ConflictException(
          String.format(Error.NON_UNIQUE, StringUtils.capitalize(Fields.key),
              key));
    });
  }

  private Setting findExistingByIdentifier(UUID identifier) {
    return settingRepository.findById(identifier).orElseThrow(() -> new NotFoundException(
        Pair.of(Setting.Fields.identifier, identifier),
        GeographicLevel.class)
    );
  }
}
