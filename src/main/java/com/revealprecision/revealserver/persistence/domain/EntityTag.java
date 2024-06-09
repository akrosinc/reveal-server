package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;

@FieldNameConstants
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntityTag {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(unique = true)
  private String tag;

  private String valueType;

  private String definition;

  @Type(type = "list-array")
  private List<String> aggregationMethod;

  private boolean isAggregate;

  private boolean simulationDisplay;

  @ManyToOne
  @JoinColumn(name = "metadata_import_id", referencedColumnName = "identifier")
  private MetadataImport metadataImport;

  private UUID referencedTag;

  @Column(name = "is_public")
  private boolean isPublic;

  @OneToMany(mappedBy = "entityTag",cascade = CascadeType.ALL)
  private List<EntityTagAccGrantsOrganization> entityTagAccGrantsOrganizations;


  @OneToMany(mappedBy = "entityTag",cascade = CascadeType.ALL)
  private List<EntityTagAccGrantsUser> entityTagAccGrantsUsers;

  @OneToMany(mappedBy = "entityTag",cascade = CascadeType.ALL)
  private List<EntityTagOwnership> owners;

  private boolean isDeleting;

}
