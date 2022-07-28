package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.metadata.LocationMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataList;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Where(clause = "entity_status='ACTIVE'")
public class MetadataImport extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(nullable = false)
  private String filename;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
  @Column(nullable = false)
  private LocalDateTime uploadedDatetime;

  @Column(nullable = false)
  private String uploadedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BulkEntryStatus status;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Set<LocationMetadataEvent> locationMetadataEvents = new HashSet<>();
}
