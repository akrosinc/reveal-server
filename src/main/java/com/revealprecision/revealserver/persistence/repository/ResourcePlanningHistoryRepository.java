package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.api.v1.dto.response.ResourcePlanningHistoryResponse;
import com.revealprecision.revealserver.persistence.domain.ResourcePlanningHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourcePlanningHistoryRepository extends JpaRepository<ResourcePlanningHistory, UUID> {

  @Query(value = "select new com.revealprecision.revealserver.api.v1.dto.response.ResourcePlanningHistoryResponse(rh.identifier, rh.name, u.username, rh.createdDatetime) from ResourcePlanningHistory rh "
      + "left join User u on CAST(u.sid as string) = rh.createdBy ")
  Page<ResourcePlanningHistoryResponse> getHistory(Pageable pageable);
//
  int countByBaseName(String name);

  Optional<ResourcePlanningHistory> getResourcePlanningHistoryByName(String name);

}
