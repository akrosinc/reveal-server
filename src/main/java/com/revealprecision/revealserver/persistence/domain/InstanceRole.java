package com.revealprecision.revealserver.persistence.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "instance_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class InstanceRole {
  @Id
  @GeneratedValue
  private UUID identifier;
  private String name;
  //permissions
  @OneToMany(
      mappedBy = "instanceRole",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private Set<InstanceRolePermission> permissions = new HashSet<>();
}
