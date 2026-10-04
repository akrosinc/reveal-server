package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceUserId;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "instance_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class InstanceUser {

  @EmbeddedId
  private InstanceUserId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("instanceId")
  @JoinColumn(name = "instance_id", nullable = false)
  private Instance instance;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "instance_role_id")
  private UUID instanceRoleId;

  public void populate(final Instance instance, final User user) {
    this.instance = instance;
    this.user = user;
    this.id = new InstanceUserId(instance.getIdentifier(), user.getIdentifier());
  }
}
