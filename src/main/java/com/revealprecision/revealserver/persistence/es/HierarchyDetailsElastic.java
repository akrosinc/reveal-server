package com.revealprecision.revealserver.persistence.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class HierarchyDetailsElastic {

  @Field(type = FieldType.Keyword)
  private String parent;

  @Field(type = FieldType.Integer)
  private int geographicLevelNumber;

  @Field(type = FieldType.Flattened)
  private List<String> ancestry;

}
