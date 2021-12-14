package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends EntityGraphJpaRepository<User, UUID> {

  Optional<User> findByUserName(String username);

  @Query(value = "SELECT * FROM users usr "
      + "LEFT JOIN user_groups ug ON usr.identifier = ug.user_id "
      + "LEFT JOIN organization org ON org.identifier = ug.organization_id "
      + "WHERE usr.user_name LIKE :param "
      + "OR usr.first_name LIKE :param "
      + "OR usr.last_name LIKE :param "
      + "OR usr.email LIKE :param "
      + "OR org.name LIKE :param", nativeQuery = true)
  Page<User> searchByParameter(@Param("param") String param, Pageable pageable,
      EntityGraph entityGraph);
}
