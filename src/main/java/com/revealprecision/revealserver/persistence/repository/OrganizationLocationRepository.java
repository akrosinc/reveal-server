package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationLocationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationLocationRepository extends
    JpaRepository<OrganizationLocation, OrganizationLocationId> {

}
