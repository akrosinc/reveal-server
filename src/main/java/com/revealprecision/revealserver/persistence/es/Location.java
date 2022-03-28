package com.revealprecision.revealserver.persistence.es;

import com.revealprecision.revealserver.persistence.domain.Geometry;
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
@Document(indexName = "location")
public class Location {

  @Id
  private String id;

  @Field(type = FieldType.Text)
  private String level;

  @GeoShapeField
  Geometry geometry;
}
