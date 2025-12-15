package com.revealprecision.revealserver.amdr.persistence.repository;

import com.revealprecision.revealserver.amdr.persistence.domain.AmdrData;
import com.revealprecision.revealserver.amdr.persistence.projection.AmdrEventSampleProjection;
import com.revealprecision.revealserver.amdr.persistence.projection.AmdrPassiveEventProjection;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AmdrRepository extends JpaRepository<AmdrData, UUID> {

  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY amdr.amdr_data_calc_mat", nativeQuery = true)
  @Modifying
  @Transactional
  void refreshAmdrImportData();

  @Query(value = "call amdr.refresh_amdr_summary()", nativeQuery = true)
  @Modifying
  @Transactional
  void summarizeImportData();


  @Query(value = "SELECT l.name as locationName,t.barcode,t.capture_datetime as captureDatetime from (\n"
      + "                  SELECT e.location_identifier,e.capture_datetime, arr.*\n"
      + "                  from event e,\n"
      + "                       LATERAL extract_obs_fields_parasitology(e.additional_information) with ordinality arr\n"
      + "                  where e.event_type = 'parasitology'\n"
      + "                    and arr.microscopy_results = 'positive'\n"
      + "                    and e.location_identifier is not null\n"
      + ") as  t\n"
      + "left join location l on l.identifier = t.location_identifier WHERE t.barcode = :sample",nativeQuery = true)
  List<AmdrEventSampleProjection> getSampleData(String sample);


  @Query(value = "SELECT p.barcode, cast(pl.identifier as varchar) AS locationIdentifier, pl.name AS locationName\n"
      + "FROM (\n"
      + "         SELECT DISTINCT t.val ->> 'rcd_barcode' as barcode\n"
      + "                       , CASE\n"
      + "                             WHEN t.val ->> 'cluster' IS NOT NULL AND l.name IS NOT NULL\n"
      + "                                 THEN l.identifier\n"
      + "                             WHEN t.val ->> 'cluster' IS NULL AND ll.name IS NOT NULL AND\n"
      + "                                  ll.location_property ->> 'geographicLevel' = 'cluster'\n"
      + "                                 THEN ll.identifier\n"
      + "                             ELSE NULL\n"
      + "             END                                 AS locationIdentifier\n"
      + "                       , CASE\n"
      + "                             WHEN t.val ->> 'cluster' IS NOT NULL AND l.name IS NOT NULL\n"
      + "                                 THEN t.val ->> 'cluster'\n"
      + "                             WHEN t.val ->> 'cluster' IS NULL AND ll.name IS NOT NULL AND\n"
      + "                                  ll.location_property ->> 'geographicLevel' = 'cluster'\n"
      + "                                 THEN ll.name\n"
      + "                             ELSE NULL\n"
      + "             END                                 AS locationName\n"
      + "         from (\n"
      + "                  SELECT e.identifier,\n"
      + "                         e.location_identifier,\n"
      + "                         arr.*\n"
      + "                  from event e,\n"
      + "                       lateral amdr.extract_obs_fields_dynamic(e.additional_information,\n"
      + "                                                               CAST(ARRAY ['cluster','rcd_barcode'] as text[])) with ordinality arr(val, pos)\n"
      + "                  WHERE e.event_type = 'passive_case_detection'\n"
      + "              ) t\n"
      + "                  left join location l on l.name = t.val ->> 'cluster'\n"
      + "                  left join location ll on t.location_identifier = ll.identifier\n"
      + "         WHERE t.val ->> 'rcd_barcode' IS NOT NULL\n"
      + "           and t.val ->> 'rcd_barcode' = :sample\n"
      + "     ) as p\n"
      + "         left join location_relationship lr on lr.location_identifier = p.locationIdentifier\n"
      + "         left join location_relationship plr on plr.location_identifier = lr.parent_identifier\n"
      + "         left join location pl on plr.parent_identifier = pl.identifier\n",nativeQuery = true)
  List<AmdrPassiveEventProjection> getPassiveCaseSampleData(String sample);



}
