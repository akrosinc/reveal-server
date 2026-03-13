package com.revealprecision.revealserver.persistence.repository;


import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceRoleRepository extends JpaRepository<InstanceRole, UUID> {
  Optional<InstanceRole> findByName(String name);


}
