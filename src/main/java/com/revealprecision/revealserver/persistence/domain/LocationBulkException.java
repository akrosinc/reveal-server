package com.revealprecision.revealserver.persistence.domain;


import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class LocationBulkException extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  private String message;

  @ManyToOne
  @JoinColumn(name = "location_bulk_identifier")
  private LocationBulk locationBulk;
}
