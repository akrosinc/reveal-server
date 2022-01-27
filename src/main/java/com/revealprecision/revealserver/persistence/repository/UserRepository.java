package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends EntityGraphJpaRepository<User, UUID> {

  @Query(value = "select * from users u where u.username = :username and u.entity_status != 'DELETED'", nativeQuery = true)
  Optional<User> getByUsername(@Param("username") String username);

  @Query(value = "select * from users u where u.email = :email and u.entity_status != 'DELETED'", nativeQuery = true)
  Optional<User> findByEmail(@Param("email") String email);

  @Query(value = "select u from User u "
      + "where (u.username = :username "
      + "or lower(u.email) = lower(:email)) "
      + "and u.entityStatus != com.revealprecision.revealserver.enums.EntityStatus.DELETED")
  Optional<User> findByUserNameOrEmail(@Param("username") String username,
      @Param("email") String email);

  @Query(value = "select u from User u "
      + "left join u.organizations org "
      + "where lower(u.username) like lower(concat('%', :param, '%')) "
      + "OR lower(u.firstName) like lower(concat('%', :param, '%')) "
      + "OR lower(u.lastName) like lower(concat('%', :param, '%')) "
      + "OR lower(u.email) like lower(concat('%', :param, '%')) "
      + "OR lower(org.name) like lower(concat('%', :param, '%'))",
      countQuery = "select count(distinct usr) from User usr "
          + "left join usr.organizations org "
          + "where lower(usr.username) like lower(concat('%', :param, '%')) "
          + "OR lower(usr.firstName) like lower(concat('%', :param, '%')) "
          + "OR lower(usr.lastName) like lower(concat('%', :param, '%')) "
          + "OR lower(usr.email) like lower(concat('%', :param, '%')) "
          + "OR lower(org.name) like lower(concat('%', :param, '%'))")
  Page<User> searchByParameter(@Param("param") String param, Pageable pageable,
      EntityGraph entityGraph);

  Optional<User> findByIdentifier(UUID identifier);

  Optional<User> findBySid(UUID sid);

  @Transactional
  @Modifying
  @Query("UPDATE User usr SET usr.apiResponse = :message WHERE usr.identifier = :id")
  void setApiResponse(@Param("id") UUID id, @Param("message") String message);

  @Query(value = "select u.email from users u where u.entity_status != 'DELETED' and u.email is not null", nativeQuery = true)
  List<String> getAllEmails();

  @Query(value = "select u.username from users u where u.entity_status != 'DELETED'", nativeQuery = true)
  List<String> getAllUsernames();
}
