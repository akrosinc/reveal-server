package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.SettingConfigurationResponse;
import com.revealprecision.revealserver.persistence.domain.Setting;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingConfigurationResponseFactory {

  public static SettingConfigurationResponse fromSettingsAndSettingTypeIdentifier(List<Setting> settings,String settingTypeIdentifier) {
    SettingConfigurationResponse settingConfigurationResponse = SettingConfigurationResponse
        .builder().settings(settings.stream().map(SettingResponseFactory::fromEntity).collect(
            Collectors.toList())).build();
    return settingConfigurationResponse;
  }

}
