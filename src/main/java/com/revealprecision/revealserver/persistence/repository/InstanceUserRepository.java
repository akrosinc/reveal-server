package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Instance;
import com.revealprecision.revealserver.persistence.domain.InstanceUser;
import com.revealprecision.revealserver.persistence.domain.id.InstanceUserId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstanceUserRepository  extends
    JpaRepository<InstanceUser, InstanceUserId> {

  void deleteByInstance(Instance instance);

  @Query("SELECT iu.instance FROM InstanceUser iu WHERE iu.user.identifier = :identifier")
  List<Instance> getUserInstances(UUID identifier);
}
