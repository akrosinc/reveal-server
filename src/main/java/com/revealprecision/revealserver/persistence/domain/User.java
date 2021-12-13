package com.revealprecision.revealserver.persistence.domain;

import java.util.Set;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
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
@SQLDelete(sql = "UPDATE user SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class User extends AbstractAuditableEntity {

  @ManyToMany
  @JoinTable(
      name = "user_groups",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "organization_id"))
  Set<Organization> organizations;
  @Id
  @GeneratedValue
  private UUID identifier;
  @Column(nullable = false)
  private String firstName;
  @Column(nullable = false)
  private String lastName;
  @Column(nullable = false)
  private String userName;
  @Column(nullable = false)
  @Email
  private String email;
  @ElementCollection
  @CollectionTable(name = "user_security_groups", joinColumns = @JoinColumn(name = "identifier"))
  @Column(name = "security_group")
  private Set<String> securityGroups;
}
