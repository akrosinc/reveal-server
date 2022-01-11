package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.UserBulk;
import com.revealprecision.revealserver.persistence.projection.UserBulkProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserBulkRepository extends JpaRepository<UserBulk, UUID> {

    @Query(value = "select ube.username as username, ube.message as message, null as entityStatus " +
            "from user_bulk_exception ube " +
            "where ube.user_bulk_identifier = :identifier " +
            "UNION " +
            "select usr.user_name as username, usr.api_response as message, usr.entity_status as entityStatus " +
            "from users usr " +
            "where usr.user_bulk_identifier = :identifier",
            countQuery = "select count(*) from (select ube.username as username, ube.message as message, null as entityStatus " +
                    "from user_bulk_exception ube " +
                    "where ube.user_bulk_identifier = :identifier " +
                    "UNION " +
                    "select usr.user_name as username, usr.api_response as message, usr.entity_status as entityStatus " +
                    "from users usr " +
                    "where usr.user_bulk_identifier = :identifier) bulk", nativeQuery = true)
    Page<UserBulkProjection> findBulkById(@Param("identifier") UUID identifier, Pageable page);
}
