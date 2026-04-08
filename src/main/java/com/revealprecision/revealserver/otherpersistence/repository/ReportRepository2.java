package com.revealprecision.revealserver.otherpersistence.repository;

import com.revealprecision.revealserver.otherpersistence.domain.Report2;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository2 extends JpaRepository<Report2, UUID> {

}
