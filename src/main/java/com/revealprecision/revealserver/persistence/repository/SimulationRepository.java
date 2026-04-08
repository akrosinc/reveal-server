package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, UUID> {

    @Query("SELECT s FROM Simulation s WHERE s.plan.identifier = :planId")
    Optional<Simulation> findByPlanIdentifier(@Param("planId") UUID planId);
}
