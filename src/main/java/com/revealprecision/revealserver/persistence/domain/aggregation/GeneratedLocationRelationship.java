package com.revealprecision.revealserver.persistence.domain.aggregation;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@TypeDef(
    name = "list-array",
    typeClass = ListArrayType.class
)
public class GeneratedLocationRelationship {

  @Id
  @GeneratedValue
  private int id;

  private String locationIdentifier;

  private String parentIdentifier;

  @Type(type = "list-array")
  private List<String> ancestry;

  @ManyToOne
  private GeneratedHierarchy generatedHierarchy;
  
}
