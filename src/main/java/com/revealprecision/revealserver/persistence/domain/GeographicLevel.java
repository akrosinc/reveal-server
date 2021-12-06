package com.revealprecision.revealserver.persistence.domain;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.UUID;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NamedNativeQueries(
    @NamedNativeQuery(
        name = "findByName",
        query = "select * from geographic_level where name = ?",
        resultClass = GeographicLevel.class
    )
)
public class GeographicLevel extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @NotBlank(message = "must not be empty")
  private String title;

  @Pattern(regexp = "[a-z0-9\\-]+", message = "pattern not matched")
  @NotBlank(message = "must not be empty")
  private String name;

  public GeographicLevel update(GeographicLevel request) {
    this.name = request.getName();
    this.title = request.getTitle();
    return this;
  }
}
