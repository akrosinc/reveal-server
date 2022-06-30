package com.revealprecision.revealserver.persistence.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.revealprecision.revealserver.persistence.domain.Geometry;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoShapeField;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "location")
public class LocationElastic {

  @Id
  private String id;

  @Field(type = FieldType.Text)
  private String level;

  @Field(type = FieldType.Text)
  private String name;

  @Field(type = FieldType.Text)
  private String externalId;

  @Field(type = FieldType.Flattened)
  private List<PersonElastic> person;

  @GeoShapeField
  Geometry geometry;
}
