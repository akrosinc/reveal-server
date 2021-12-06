package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.GeographicLevelRequest;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeographicLevel extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @NotBlank(message = "must not be empty")
  private String title;

  @Pattern(regexp = "[a-z0-9\\-]+", message = "pattern not matched")
  @NotBlank(message = "must not be empty")
  @Column(unique = true)
  private String name;

  public GeographicLevel update(GeographicLevelRequest request) {
    this.name = request.getName();
    this.title = request.getTitle();
    return this;
  }
}