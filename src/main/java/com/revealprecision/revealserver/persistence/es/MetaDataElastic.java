package com.revealprecision.revealserver.persistence.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class MetaDataElastic {

  private String planId;
  private String userId;
  private Date createDateTime;
  private Date updateDateTime;
  private String taskId;
  private String taskType;

}
