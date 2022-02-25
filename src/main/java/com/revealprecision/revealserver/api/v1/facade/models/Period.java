package com.revealprecision.revealserver.api.v1.facade.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Period implements Serializable {

  private static final long serialVersionUID = 1L;
  private String start;
  private String end;

  public static Period between(String start,String end){
    return new Period(start,end);
  }
}
