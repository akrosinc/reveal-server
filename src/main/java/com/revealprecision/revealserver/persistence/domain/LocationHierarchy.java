package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.UniqueElements;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(
    name = "list-array",
    typeClass = ListArrayType.class
)
@SQLDelete(sql = "UPDATE location_hierarchy SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class LocationHierarchy extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @NotEmpty(message = "node order list  is required and must not be empty")
  @UniqueElements(message = "duplicate nodes in hierarchy")
  @Type(type = "list-array")
  private List<String> nodeOrder;

  @OneToMany(mappedBy = "locationHierarchy")
  private List<LocationRelationship> locationRelationships = new ArrayList<>();
}
