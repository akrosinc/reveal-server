package com.revealprecision.revealserver.api.v1.facade.models;

import java.sql.Time;
import java.time.DayOfWeek;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimingRepeatFacade {

  private int count;
  private int countMax;
  private float duration;
  private float durationMax;
  private DurationCode code;
  private int frequency;
  private int frequencyMax;
  private float period;
  private float periodMax;
  private DurationCode periodUnit;
  private List<DayOfWeek> dayOfWeek; //maybe change?
  private List<Time> timeOfDay;
  private List<String> when;
  private int offset;

  public enum DurationCode {
    s, min, h, d, wk, mo, a;
  }
}