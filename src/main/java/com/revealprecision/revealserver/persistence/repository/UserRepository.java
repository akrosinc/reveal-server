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

  @Query(value = "select u from User u "
      + "left join u.organizations org "
      + "where u.userName like %:param% "
      + "OR u.firstName like %:param% "
      + "OR u.lastName like %:param% "
      + "OR u.email like %:param% "
      + "OR org.name like %:param%")
  Page<User> searchByParameter(@Param("param") String param, Pageable pageable,
      EntityGraph entityGraph);

  Optional<User> findByIdentifier(UUID identifier);
}
