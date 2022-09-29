package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignDrugRepository extends JpaRepository<CampaignDrug, UUID> {

  @Query(value = "select c from CampaignDrug c where c.identifier in :identifiers")
  List<CampaignDrug> getAllByIdentifiers(Set<UUID> identifiers);
}
