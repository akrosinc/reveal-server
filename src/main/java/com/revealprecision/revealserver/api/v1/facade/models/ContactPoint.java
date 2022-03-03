package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.james.mime4j.field.datetime.DateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactPoint implements Serializable {

  private static final long serialVersionUID = 1L;

  private String type;

  private String use;

  private String number;

  private int preference;

  private DateTime startDate;

  private DateTime endDate;


}
