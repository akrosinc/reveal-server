package com.revealprecision.revealserver.api.v1.facade.request;

import java.io.Serializable;
import lombok.Data;

@Data
public class HdssSearchRequest implements Serializable  {

  private String searchString;

  private String gender;
}
