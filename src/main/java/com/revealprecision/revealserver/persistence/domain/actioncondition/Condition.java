package com.revealprecision.revealserver.persistence.domain.actioncondition;

import java.util.List;
import lombok.Data;

@Data
public class Condition {
  String entity;
  String type;
  String dataType;
  String operator;
  String property;
  List<String> value;
  String group;
}

