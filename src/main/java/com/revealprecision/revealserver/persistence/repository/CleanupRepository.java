package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import com.revealprecision.revealserver.persistence.projection.TaskDataFromEventProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@Profile("cleanup")
public interface CleanupRepository extends JpaRepository<TaskBusinessStateTracker, UUID> {

  @Query(value = "SELECT CAST(tt.event_id as varchar) as eventId\n"
      + ",CAST(tt.task_identifier as varchar) as taskIdentifier\n"
      + ",CAST(tt.field_code as varchar) as fieldCode\n"
      + ",CAST(tt.business_status as varchar) as eventBusinessStatus\n"
      + ",CAST(t.business_status as varchar) as taskBusinessStatus\n"
      + ",CAST(lts.code as varchar) as taskStatus\n" + "      from (\n"
      + "SELECT ee.e_id as event_id,ee.capture_datetime as capture_datetime,ee.task_identifier as task_identifier,ee.item_object ->> 'fieldCode'  as field_code, ee.item_object -> 'values' ->> 0 as business_status from \n"
      + "(SELECT e.identifier as e_id,e.server_version as server_version,e.task_identifier as task_identifier, arr.item_object as item_object, e.capture_datetime as capture_datetime\n"
      + " From event e, jsonb_array_elements(e.additional_information -> 'obs') \n"
      + " with ordinality arr(item_object , position)  ) as ee\n" + "      inner join \n"
      + "(SELECT e.task_identifier as task_identifier, max(e.server_version) as server_version \n"
      + " From event e WHERE e.task_identifier is not null \n"
      + " group by e.task_identifier ) as tt on tt.task_identifier  = ee.task_identifier \n"
      + "WHERE ee.server_version = tt.server_version and ee.item_object ->> 'fieldCode' = 'business_status'\n"
      + ") as tt \n" + "      left join  task t on t.identifier = tt.task_identifier\n"
      + "left join lookup_task_status lts on lts.identifier = t.lookup_task_status_identifier\n"
      + "WHERE t.business_status != tt.business_status and tt.capture_datetime > t.modified_datetime + interval '2 hour'", nativeQuery = true)
  List<TaskDataFromEventProjection> getBusinessStatusFromEventWhereTaskDiffersFromEvent();


  @Query(value = "SELECT DISTINCT CAST(ee.identifier as varchar) as eventId From event ee\n"
      + "inner join (\n"
      + "SELECT   e.base_entity_identifier,max(e.server_version) as server_version  from event e \n"
      + "\t group by e.base_entity_identifier\n"
      + "\t) as e on e.server_version = ee.server_version\n"
      + "left join report r on r.location_id = e.base_entity_identifier\n"
      + "left join location l on l.identifier = e.base_entity_identifier\n"
      + "left join geographic_level gl on gl.identifier = l.geographic_level_identifier \n"
      + "WHERE r.id is null and gl.name = 'structure'", nativeQuery = true)
  List<String> getEventIdsNotSentToReport();

  @Query(value = "SELECT CAST(tt.event_id as varchar) as eventId from (\n"
      + "SELECT ee.e_id as event_id, ee.task_identifier as  task_identifier ,ee.capture_datetime as capture_datetime,ee.base_entity_identifier as base_entity_identifier,ee.item_object ->> 'fieldCode'  as field_code, ee.item_object -> 'values' ->> 0 as business_status from \n"
      + "\t(SELECT e.identifier as e_id,e.server_version as server_version, e.base_entity_identifier, arr.item_object as item_object, e.capture_datetime as capture_datetime, e.task_identifier\n"
      + "\t \tFrom event e, jsonb_array_elements(e.additional_information -> 'obs') \n"
      + " \twith ordinality arr(item_object , position)  ) as ee\n" + "inner join \n"
      + "\t(SELECT e.task_identifier as task_identifier, max(e.server_version) as server_version \n"
      + "\t From event e WHERE e.task_identifier is not null \n"
      + "\t group by e.task_identifier ) as tt on tt.task_identifier  = ee.task_identifier \n"
      + "WHERE ee.server_version = tt.server_version and ee.item_object ->> 'fieldCode' = 'business_status'\n"
      + ") as tt \n" + "left join report r on r.location_id = tt.base_entity_identifier\n"
      + "left join location l on l.identifier = r.location_id\n"
      + "left join geographic_level gl on gl.identifier = l.geographic_level_identifier \n"
      + "WHERE (r.report_indicators ->>'businessStatus' != tt.business_status and gl.name = 'structure')", nativeQuery = true)
  List<String> getEventIdsWhereBusinessStatusNotMatchingToReport();
}
