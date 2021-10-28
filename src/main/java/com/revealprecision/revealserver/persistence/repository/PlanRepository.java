package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanRepository {

    List<Plan> searchPlans(String planTitle);

    @Query(value = "SELECT * FROM plan WHERE payload ->> 'title' LIKE '%:planTitle%;",
        countQuery = "SELECT COUNT(*) FROM plan WHERE payload ->> 'title' LIKE '%:planTitle%;",
        nativeQuery = true)
    Page<Plan> searchPlans(@Param("planTitle") String planTitle, Pageable PageRequest);
}