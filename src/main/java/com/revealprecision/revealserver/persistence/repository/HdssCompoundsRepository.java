package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.projection.HdssCompoundHouseholdIndividualProjection;
import com.revealprecision.revealserver.persistence.projection.HdssCompoundHouseholdProjection;
import com.revealprecision.revealserver.persistence.projection.HdssCompoundProjection;
import com.revealprecision.revealserver.persistence.projection.HdssHouseholdIndividualProjection;
import com.revealprecision.revealserver.persistence.projection.HdssHouseholdStructureProjection;
import com.revealprecision.revealserver.persistence.projection.HdssIndividualProjection;
import com.revealprecision.revealserver.persistence.domain.HdssCompounds;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HdssCompoundsRepository extends EntityGraphJpaRepository<HdssCompounds, UUID> {

  @Query(value = "SELECT DISTINCT compound_id as compoundId from hdss.hdss_compounds hc  ", nativeQuery = true)
  List<HdssCompoundProjection> getAllCompounds();

  @Query(value = "SELECT DISTINCT compound_id as compoundId from hdss.hdss_compounds hc WHERE hc.server_version>=:server_version ", nativeQuery = true)
  List<HdssCompoundProjection> getAllCompoundsByServerVersion();

  @Query(value = "SELECT DISTINCT household_id as householdId, compound_id as compoundId from hdss.hdss_compounds hc WHERE hc.server_version>=:server_version", nativeQuery = true)
  List<HdssCompoundHouseholdProjection> getCompoundHouseHolds();

  @Query(value = "SELECT DISTINCT household_id as householdId, compound_id as compoundId from hdss.hdss_compounds hc LIMIT 50", nativeQuery = true)
  List<HdssCompoundHouseholdProjection> getCompoundHouseHoldsByServerVersion();


  @Query(value = "SELECT DISTINCT household_id as householdId, compound_id as compoundId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList", nativeQuery = true)
  List<HdssCompoundHouseholdProjection> getCompoundHouseHoldsByCompoundIdIn(List<String> compoundIdList);

  @Query(value = "SELECT DISTINCT hc.server_version as serverVersion,household_id as householdId, compound_id as compoundId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList and hc.server_version>=:serverVersion", nativeQuery = true)
  List<HdssCompoundHouseholdProjection> getCompoundHouseHoldsByCompoundIdInAndServerVersion(List<String> compoundIdList,long serverVersion);


  @Query(value = "SELECT DISTINCT household_id as householdId, individual_id as individualId from hdss.hdss_compounds hc LIMIT 100", nativeQuery = true)
  List<HdssHouseholdIndividualProjection> getAllHouseholdIndividual();

  @Query(value = "SELECT DISTINCT household_id as householdId, individual_id as individualId from hdss.hdss_compounds hc WHERE hc.server_version>=:server_version", nativeQuery = true)
  List<HdssHouseholdIndividualProjection> getAllHouseholdIndividualByServerVersion();

  @Query(value = "SELECT DISTINCT household_id as householdId, individual_id as individualId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList", nativeQuery = true)
  List<HdssHouseholdIndividualProjection> getAllHouseholdIndividualByCompoundIdIn(List<String> compoundIdList);

  @Query(value = "SELECT DISTINCT hc.server_version as serverVersion,household_id as householdId, individual_id as individualId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList and hc.server_version>=:serverVersion", nativeQuery = true)
  List<HdssHouseholdIndividualProjection> getAllHouseholdIndividualByCompoundIdInAndServerVersion(List<String> compoundIdList, long serverVersion);


  @Query(value = "SELECT DISTINCT household_id as householdId, cast(structure_id as varchar) as structureId from hdss.hdss_compounds hc LIMIT 100", nativeQuery = true)
  List<HdssHouseholdStructureProjection> getAllHouseholdStructures();

  @Query(value = "SELECT DISTINCT household_id as householdId, cast(structure_id as varchar) as structureId from hdss.hdss_compounds hc WHERE hc.server_version>=:serverVersion", nativeQuery = true)
  List<HdssHouseholdStructureProjection> getAllHouseholdStructuresByServerVersion();

  @Query(value = "SELECT DISTINCT household_id as householdId, cast(structure_id as varchar) as structureId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList", nativeQuery = true)
  List<HdssHouseholdStructureProjection> getAllHouseholdStructuresByCompoundIdIn(List<String> compoundIdList);

  @Query(value = "SELECT DISTINCT hc.server_version as serverVersion,household_id as householdId, cast(structure_id as varchar) as structureId from hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList and hc.server_version>=:serverVersion", nativeQuery = true)
  List<HdssHouseholdStructureProjection> getAllHouseholdStructuresByCompoundIdInAndServerVersion(List<String> compoundIdList,long serverVersion);

  @Query(value = "SELECT DISTINCT hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc LIMIT 400", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividuals();

  @Query(value = "SELECT DISTINCT hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.server_version>=:server_version", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsByServerVersion();

  @Query(value = "SELECT DISTINCT cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsByCompoundIdIn(List<String> compoundIdList);

  @Query(value = "SELECT DISTINCT hc.server_version as serverVersion, cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList and hc.server_version>=:serverVersion", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsByCompoundIdInAndServerVersion(
      List<String> compoundIdList, long serverVersion);


  @Query(value = "SELECT DISTINCT hc.server_version as serverVersion, cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.compound_id in :compoundIdList and hc.server_version>=:serverVersion LIMIT :size", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsByCompoundIdInAndServerVersionAndSize(List<String> compoundIdList, long serverVersion, int size);


  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + " hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob\n"
      + " from hdss.hdss_compounds hc\n"
      + " WHERE (hc.household_id like upper(concat('%',:searchString,'%'))\n"
      + "           or hc.compound_id like upper(concat('%',:searchString,'%')) or hc.individual_id like upper(concat('%',:searchString,'%')))\n", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithString(String searchString);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + " hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob\n"
      + " from hdss.hdss_compounds hc\n"
      + " WHERE hc.server_version >= :serverVersion"
      + " LIMIT :batchSize", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> getAllBySizeAndServerVersion(int batchSize, long serverVersion);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE (hc.household_id like upper(concat('%',:searchString,'%'))\n"
      + "           or hc.compound_id like upper(concat('%',:searchString,'%')) or hc.individual_id like upper(concat('%',:searchString,'%')))\n"
      + "and upper(hc.fields->>'gender') =upper(:gender)", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithStringAndGender(String searchString, String gender);


  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE (hc.household_id like upper(concat('%',:searchString,'%'))\n"
      + "           or hc.compound_id like upper(concat('%',:searchString,'%')) or hc.individual_id like upper(concat('%',:searchString,'%')))\n"
      + "and upper(hc.fields->>'gender') =upper(:gender) and hc.fields->>'dob' = :dob ", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithStringGenderAndDob(String searchString, String gender, String dob);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE (hc.household_id like upper(concat('%',:searchString,'%'))\n"
      + "           or hc.compound_id like upper(concat('%',:searchString,'%')) or hc.individual_id like upper(concat('%',:searchString,'%')))\n"
      + "and hc.fields->>'dob' = :dob ", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithStringAndDob(String searchString, String dob);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE upper(hc.fields->>'gender') =upper(:gender) and hc.fields->>'dob' = :dob ", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithGenderAndDob(String gender, String dob);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE hc.fields->>'dob' = :dob ", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithDob(String dob);

  @Query(value = "SELECT cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId,\n"
      + "hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob \n"
      + "from hdss.hdss_compounds hc\n"
      + "WHERE upper(hc.fields->>'gender') =upper(:gender)", nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> searchWithGender(String gender);

  @Query(value = "SELECT DISTINCT  hc.compound_id as compoundId FROM\n"
      + "(SELECT lr.location_identifier as child_location, arr.ancestor\n"
      + "from location_relationship lr,\n"
      + "     unnest(lr.ancestry) with ordinality arr(ancestor, pos)\n"
      + "    ) as lr\n"
      + "left join location lp on lr.ancestor = lp.identifier\n"
      + "left join geographic_level pgl on pgl.identifier = lp.geographic_level_identifier\n"
      + "left join location lc on lc.identifier = lr.child_location\n"
      + "left join geographic_level cgl on cgl.identifier = lc.geographic_level_identifier\n"
      + "inner  join hdss.hdss_compounds hc on hc.structure_id = lr.child_location\n"
      + "WHERE lp.identifier in (\n"
      + "    SELECT\n"
      + "           l.identifier\n"
      + "    from plan_assignment pa\n"
      + "             inner join organization o on pa.organization_identifier = o.identifier\n"
      + "             left join plan_locations pl on pl.identifier = pa.plan_locations_identifier\n"
      + "             left join location l on l.identifier = pl.location_identifier\n"
      + "             left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "             left join plan p on pl.plan_identifier = p.identifier\n"
      + "             left join location_hierarchy lh on p.hierarchy_identifier = lh.identifier\n"
      + "             left join plan_target_type ptt on ptt.plan_identifier = p.identifier\n"
      + "             left join geographic_level pgl on ptt.geographic_level_identifier = pgl.identifier\n"
      + "             left join user_organization uo on o.identifier = uo.organization_identifier\n"
      + "             left join users u on u.identifier = uo.user_identifier\n"
      + "    WHERE gl.name = lh.node_order[array_position(lh.node_order, pgl.name) - 1]\n"
      + "      and u.username = :username\n"
      + "    )",nativeQuery = true)
  List<HdssCompoundProjection> getAllCompoundsForUserAssignment(String username);

  @Query(value = "SELECT DISTINCT  hc.server_version as serverVersion,hc.compound_id as compoundId FROM\n"
      + "(SELECT lr.location_identifier as child_location, arr.ancestor\n"
      + "from location_relationship lr,\n"
      + "     unnest(lr.ancestry) with ordinality arr(ancestor, pos)\n"
      + "    ) as lr\n"
      + "left join location lp on lr.ancestor = lp.identifier\n"
      + "left join geographic_level pgl on pgl.identifier = lp.geographic_level_identifier\n"
      + "left join location lc on lc.identifier = lr.child_location\n"
      + "left join geographic_level cgl on cgl.identifier = lc.geographic_level_identifier\n"
      + "inner  join hdss.hdss_compounds hc on hc.structure_id = lr.child_location and hc.server_version >=:serverVersion\n"
      + "WHERE lp.identifier in (\n"
      + "    SELECT\n"
      + "           l.identifier\n"
      + "    from plan_assignment pa\n"
      + "             inner join organization o on pa.organization_identifier = o.identifier\n"
      + "             left join plan_locations pl on pl.identifier = pa.plan_locations_identifier\n"
      + "             left join location l on l.identifier = pl.location_identifier\n"
      + "             left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "             left join plan p on pl.plan_identifier = p.identifier\n"
      + "             left join location_hierarchy lh on p.hierarchy_identifier = lh.identifier\n"
      + "             left join plan_target_type ptt on ptt.plan_identifier = p.identifier\n"
      + "             left join geographic_level pgl on ptt.geographic_level_identifier = pgl.identifier\n"
      + "             left join user_organization uo on o.identifier = uo.organization_identifier\n"
      + "             left join users u on u.identifier = uo.user_identifier\n"
      + "    WHERE gl.name = lh.node_order[array_position(lh.node_order, pgl.name) - 1]\n"
      + "      and u.username = :username\n"
      + "    )",nativeQuery = true)
  List<HdssCompoundProjection> getAllCompoundsForUserAssignmentAndServerVersion(String username, long serverVersion);


  @Query(value = "SELECT DISTINCT "
      + " cast(hc.id as varchar) as id, hc.compound_id as compoundId,hc.household_id as householdId,hc.individual_id as individualId, "
      + " hc.fields->>'gender' as gender, CAST(hc.fields->>'dob' as date)  as dob, hc.server_version as serverVersion"
      + ", cast(hc.structure_id as varchar) as structureId FROM\n"
      + "(SELECT lr.location_identifier as child_location, arr.ancestor\n"
      + "from location_relationship lr,\n"
      + "     unnest(lr.ancestry) with ordinality arr(ancestor, pos)\n"
      + "    ) as lr\n"
      + "left join location lp on lr.ancestor = lp.identifier\n"
      + "left join geographic_level pgl on pgl.identifier = lp.geographic_level_identifier\n"
      + "left join location lc on lc.identifier = lr.child_location\n"
      + "left join geographic_level cgl on cgl.identifier = lc.geographic_level_identifier\n"
      + "inner  join hdss.hdss_compounds hc on hc.structure_id = lr.child_location \n"
      + "WHERE lp.identifier in (\n"
      + "    SELECT\n"
      + "           l.identifier\n"
      + "    from plan_assignment pa\n"
      + "             inner join organization o on pa.organization_identifier = o.identifier\n"
      + "             left join plan_locations pl on pl.identifier = pa.plan_locations_identifier\n"
      + "             left join location l on l.identifier = pl.location_identifier\n"
      + "             left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "             left join plan p on pl.plan_identifier = p.identifier\n"
      + "             left join location_hierarchy lh on p.hierarchy_identifier = lh.identifier\n"
      + "             left join plan_target_type ptt on ptt.plan_identifier = p.identifier\n"
      + "             left join geographic_level pgl on ptt.geographic_level_identifier = pgl.identifier\n"
      + "             left join user_organization uo on o.identifier = uo.organization_identifier\n"
      + "             left join users u on u.identifier = uo.user_identifier\n"
      + "    WHERE gl.name = lh.node_order[array_position(lh.node_order, pgl.name) - 1]\n"
      + "      and u.username = :username\n "
      + "    ) and  hc.server_version>:serverVersion order by hc.server_version LIMIT :batchSize",nativeQuery = true)
  List<HdssCompoundHouseholdIndividualProjection> getAllCompoundsForUserAssignmentAndServerVersionAndBatchSize(String username, long serverVersion, int batchSize);


  @Query(value = "SELECT\n"
      + "    distinct u.email\n"
      + "from plan_assignment pa\n"
      + "         inner join organization o on pa.organization_identifier = o.identifier\n"
      + "         left join plan_locations pl on pl.identifier = pa.plan_locations_identifier\n"
      + "         left join location l on l.identifier = pl.location_identifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "         left join plan p on pl.plan_identifier = p.identifier\n"
      + "         left join location_hierarchy lh on p.hierarchy_identifier = lh.identifier\n"
      + "         left join plan_target_type ptt on ptt.plan_identifier = p.identifier\n"
      + "         left join geographic_level pgl on ptt.geographic_level_identifier = pgl.identifier\n"
      + "         left join user_organization uo on o.identifier = uo.organization_identifier\n"
      + "         left join users u on u.identifier = uo.user_identifier\n"
      + "inner join (\n"
      + "    SELECT lp.identifier,hc.compound_id\n"
      + "    FROM (SELECT lr.location_identifier as child_location, arr.ancestor\n"
      + "          from location_relationship lr,\n"
      + "               unnest(lr.ancestry) with ordinality arr(ancestor, pos)\n"
      + "         ) as lr\n"
      + "             left join location lp on lr.ancestor = lp.identifier\n"
      + "             left join geographic_level pgl on pgl.identifier = lp.geographic_level_identifier\n"
      + "             left join location lc on lc.identifier = lr.child_location\n"
      + "             left join geographic_level cgl on cgl.identifier = lc.geographic_level_identifier\n"
      + "             inner join hdss.hdss_compounds hc on hc.structure_id = lr.child_location\n"
      + "    WHERE hc.compound_id = :compoundId\n"
      + ") hl on hl.identifier = l.identifier\n"
      + "WHERE p.identifier= :planIdentifier",nativeQuery = true)
  List<String> getUserEmailsByCompoundIdAndPlan(String compoundId, UUID planIdentifier);

  @Query(value = "SELECT max(hc.server_version)  from hdss.hdss_compounds hc",nativeQuery = true)
  long getMaxServerVersion();

  @Query("SELECT DISTINCT  h.householdId FROM HdssCompounds h WHERE h.compoundId = :compoundId")
  List<String> getDistinctHouseholdsByCompoundId(List<String> compoundId);

  @Query("SELECT DISTINCT  h.structureId FROM HdssCompounds h WHERE h.compoundId in :compoundId")
  List<UUID> getDistinctStructuresByCompoundId(List<String> compoundId);

  @Query(value = "SELECT h.structureId  FROM HdssCompounds h WHERE h.individualId = :individualId ")
  UUID getStructureByIndividualId(String individualId);

  @Query(value = "SELECT h.householdId  FROM HdssCompounds h WHERE h.individualId = :individualId")
  String getHouseHoldByIndividualId(String individualId);

  @Query(value = "SELECT DISTINCT h.compoundId  FROM HdssCompounds h WHERE h.householdId = :householdId ")
  List<String> getDistinctCompoundsByHouseholdId(String householdId);

  @Query(value = "SELECT DISTINCT cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.household_id = :householdId", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsByHouseholdId(String householdId);

  @Query(value = "SELECT DISTINCT cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.individual_id = :individualId", nativeQuery = true)
  HdssIndividualProjection getIndividualByIndividualId(String individualId);

  @Query(value = "SELECT DISTINCT cast(hc.id as varchar) as id, hc.individual_id as individualId"
      + ",CAST(hc.fields->>'dob' as date) as dob,fields->>'gender' as gender from  hdss.hdss_compounds hc WHERE hc.compound_id in :compoundId", nativeQuery = true)
  List<HdssIndividualProjection> getAllIndividualsInCompoundId(List<String> compoundId);
}
