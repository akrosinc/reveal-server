package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.ActionTitleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormFieldRequest {

  private String name;

  private String display;

  private boolean addToMetadata;

  private String dataType;

  private String formTitle;

}
