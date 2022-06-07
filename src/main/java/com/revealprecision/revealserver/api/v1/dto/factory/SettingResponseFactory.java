package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.SettingResponse;
import com.revealprecision.revealserver.persistence.domain.Setting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingResponseFactory {

  public static SettingResponse fromEntity(Setting setting) {
    return SettingResponse.builder().id(setting.getIdentifier())
        .key(setting.getKey()).value(setting.getValue()).values(setting.getValues())
        .label(setting.getLabel()).description(setting.getDescription())
        .settingIdentifier(setting.getSettingIdentifier()).type(setting.getType()).build();
  }
}
