package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.UserConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConfigRepository extends JpaRepository<UserConfig, UUID> {

  List<UserConfig> findAllByReceived(boolean received);


  Optional<UserConfig> findFirstByUsername(String username);
}
