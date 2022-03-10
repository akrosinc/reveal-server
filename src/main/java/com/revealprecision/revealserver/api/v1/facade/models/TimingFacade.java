package com.revealprecision.revealserver.api.v1.facade.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.james.mime4j.field.datetime.DateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimingFacade {

  private List<DateTime> event;
  private TimingRepeatFacade repeat;
  private String code;
}