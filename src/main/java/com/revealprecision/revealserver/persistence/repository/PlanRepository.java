package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

//    @Modifying
//    @Query(value = "SELECT payload FROM plan WHERE payload ->> 'identifier' = ':planIdentifier'", nativeQuery = true)
//    static Plan findAllByPlanIdentifier(String planIdentifier) {
//
//        return "blue";
//    };
}