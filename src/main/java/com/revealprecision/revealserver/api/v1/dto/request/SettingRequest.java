package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingRequest {

  //   values: [{
  //            "emuhaya": [
  //                {
  //                    "code": "ZM-SOP-2019-09005",
  //                    "name": "Derrick Banda"
  //                },
  //                {
  //                    "code": "ZM-SOP-2019-09006",
  //                    "name": "Abudullah Nkhoma"
  //                },
  //                {
  //                    "code": "ZM-SOP-2019-09007",
  //                    "name": "Esau Mushanga"
  //                },
  //
  //     }]
  //


  @NotBlank
  private String key; // health_worker_supervisors
  @NotNull
  private String value; //only for one value eg passwaord
  @NotNull
  private List<Object> values; // {name: mmmm,code:cccccc}
  @NotBlank
  private String label;
  @NotBlank
  private String description;
  @NotBlank
  private String settingIdentifier;
  @NotBlank
  private String type;
}
