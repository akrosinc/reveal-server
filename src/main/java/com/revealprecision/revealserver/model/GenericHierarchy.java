package com.revealprecision.revealserver.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericHierarchy{
  String identifier;
  String name;
  List<String> nodeOrder;
}
