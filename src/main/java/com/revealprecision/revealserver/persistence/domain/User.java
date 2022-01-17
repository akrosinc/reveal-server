package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.UserUpdateRequest;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class User extends AbstractAuditableEntity {

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "user_organization",
      joinColumns = @JoinColumn(name = "user_identifier"),
      inverseJoinColumns = @JoinColumn(name = "organization_identifier"))
  Set<Organization> organizations;
  @Id
  @GeneratedValue
  private UUID identifier;
  @Column
  private UUID sid;
  @Column(nullable = false)
  private String firstName;
  @Column(nullable = false)
  private String lastName;
  @Column(nullable = false)
  private String username;
  @Column(nullable = false)
  @Email
  private String email;
  @ElementCollection
  @CollectionTable(name = "user_security_groups", joinColumns = @JoinColumn(name = "identifier"))
  @Column(name = "security_group")
  private Set<String> securityGroups;

  @ManyToOne
  @JoinColumn(name = "user_bulk_identifier")
  private UserBulk userBulk;

  private String apiResponse;

  public User updateUser(UserUpdateRequest request) {
    this.firstName = request.getFirstName();
    this.lastName = request.getLastName();
    this.email = request.getEmail();
    return this;
  }
}
