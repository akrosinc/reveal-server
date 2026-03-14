package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.id.InstanceUserId;
import com.revealprecision.revealserver.persistence.projection.IdentifierNameProjection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceUserRepository  extends
    JpaRepository<InstanceUser, InstanceUserId> {

  void deleteByInstance(Instance instance);

  @Query("SELECT iu.instance FROM InstanceUser iu WHERE iu.user.identifier = :identifier")
  List<Instance> getUserInstances(UUID identifier);

  @Query(value = "SELECT iu.instance FROM InstanceUser iu WHERE iu.user.identifier = :identifier")
  List<Instance> findFirstInstanceByUserIdentifier(UUID identifier);

  @Query("SELECT iu.instance FROM InstanceUser iu WHERE iu.user.identifier = :identifier and iu.instance.identifier = :instanceIdentifier")
  List<Instance> findFirstInstanceByUserIdentifierAndInstanceIdentifier(UUID identifier, UUID instanceIdentifier);

  @Query("SELECT iu.user.identifier AS identifier, iu.user.username AS name FROM  InstanceUser iu WHERE iu.instance.identifier = :instanceIdentifier")
  List<IdentifierNameProjection> getInstancesUsers(UUID instanceIdentifier);

  @Query("SELECT iu FROM InstanceUser iu " +
      "JOIN FETCH iu.instance " +
      "JOIN FETCH iu.role " +
      "WHERE iu.user.identifier = :userIdentifier")
  List<InstanceUser> findFirstByUserIdentifier(UUID userIdentifier);

  @Query("SELECT iu FROM InstanceUser iu " +
      "JOIN FETCH iu.instance " +
      "JOIN FETCH iu.role " +
      "WHERE iu.user.identifier = :userIdentifier " +
      "AND iu.instance.identifier = :instanceIdentifier")
  List<InstanceUser> findFirstByUserIdentifierAndInstanceIdentifier(
      UUID userIdentifier,
       UUID instanceIdentifier);
}
