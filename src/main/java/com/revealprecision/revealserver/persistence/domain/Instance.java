package com.revealprecision.revealserver.persistence.domain;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;


@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE instance SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class Instance extends AbstractAuditableEntity{

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE)
  private Set<Plan> plans = new HashSet<>();

  @OneToMany(mappedBy = "instance",cascade = CascadeType.REMOVE)
  private Set<InstanceEntityTag> entityTags = new HashSet<>();

  @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE)
  private Set<InstanceUser> users = new HashSet<>();

  @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE)
  private Set<InstanceLocation> locations = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "hierarchy_identifier")
  private LocationHierarchy locationHierarchy;

  @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private Set<Organization> organizations = new HashSet<>();

  @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE)
  private Set<InstanceComplexTag> complexTags = new HashSet<>();

}
