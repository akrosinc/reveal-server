package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.api.v1.dto.models.RowData;
import com.revealprecision.revealserver.api.v1.dto.models.TableRow;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatus;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DashboardService {


  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final LocationRelationshipService locationRelationshipService;
  private final PlanService planService;
  private final PlanLocationsService planLocationsService;

  public TableRow getRowData(UUID planIdentifier,
      UUID parentLocationIdentifier,
      UUID reportIdentifier, Boolean getChildren) {

    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, Long> countOfAssignedStructures = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent),
            QueryableStoreTypes.keyValueStore())
    );

    ReadOnlyKeyValueStore<String, Long> countOfStructuresByBusinessStatus = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap()
                .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy),
            QueryableStoreTypes.keyValueStore())
    );

    ReadOnlyKeyValueStore<String, OperationalAreaVisitedCount> countOfOperationalArea = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies),
            QueryableStoreTypes.keyValueStore())
    );

    ReadOnlyKeyValueStore<String, PersonBusinessStatus> personBusinessStatus = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus),
            QueryableStoreTypes.keyValueStore())
    );


    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    List<Location> childrenLocations = new ArrayList<>();

    if (parentLocationIdentifier == null) {
      List<PlanLocations> planLocations = planLocationsService.getPlanLocationsByPlanIdentifier(
          planIdentifier);
      LocationRelationship locationRelationshipfetched = planLocations.stream().map(
              planLocation -> locationRelationshipService.getLocationRelationshipsForLocation(
                  plan.getLocationHierarchy().getIdentifier(),
                  planLocation.getLocation().getIdentifier()))
          .filter(locationRelationship -> locationRelationship.getParentLocation() == null)
          .findFirst().orElseThrow(() -> new NotFoundException("no parent"));

      parentLocationIdentifier = locationRelationshipfetched.getLocation().getIdentifier();

    }

    if (getChildren) {
      childrenLocations = locationRelationshipService.getChildrenLocations(
          plan.getLocationHierarchy().getIdentifier(), parentLocationIdentifier);
    } else {
      childrenLocations = List.of(locationService.findByIdentifier(parentLocationIdentifier));
    }

    List<RowData> rowDatas = childrenLocations.stream().map(childLocation -> {

      String totalStructuresQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
      Long totalStructureCountObj = countOfAssignedStructures.get(totalStructuresQueryKey);
      double totalStructureCount = 0;
      if (totalStructureCountObj != null) {
        totalStructureCount = totalStructureCountObj;
      }
      ColumnData totalStructuresColumnData = new ColumnData();
      totalStructuresColumnData.setValue(totalStructureCount);
      totalStructuresColumnData.setIsPercentage(false);

      String notVisitedStructuresQueryKey =
          planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
              .getIdentifier() + "_" + "Not Visited";
      Long notVisitedStructuresCountObj = countOfStructuresByBusinessStatus.get(
          notVisitedStructuresQueryKey);
      double notVisitedStructuresCount = 0;
      if (notVisitedStructuresCountObj != null) {
        notVisitedStructuresCount = notVisitedStructuresCountObj;
      }

      String notEligibleStructuresQueryKey =
          planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
              .getIdentifier() + "_" + "Not Eligible";
      Long notEligibleStructuresObj = countOfStructuresByBusinessStatus.get(
          notEligibleStructuresQueryKey);
      double notEligibleStructures = 0;
      if (notEligibleStructuresObj != null) {
        notEligibleStructures = notEligibleStructuresObj;
      }


      double totalStructuresFound =
          totalStructureCount - (notVisitedStructuresCount + notEligibleStructures);

      ColumnData totalStructuresFoundColumnData = new ColumnData();
      totalStructuresFoundColumnData.setValue(totalStructuresFound);
      totalStructuresFoundColumnData.setIsPercentage(false);

      double totalFoundCoverage =
          totalStructureCount > 0 ? totalStructuresFound / totalStructureCount * 100 : 0;

      ColumnData totalFoundCoverageColumnData = new ColumnData();
      totalFoundCoverageColumnData.setValue(totalFoundCoverage);
      totalFoundCoverageColumnData.setIsPercentage(true);

      String operationalAreaVisitedQueryKey = planIdentifier + "_" + childLocation.getIdentifier();
      OperationalAreaVisitedCount operationalAreaVisitedObj = countOfOperationalArea.get(operationalAreaVisitedQueryKey);
      double operationalAreaVisitedCount = 0;
      if (operationalAreaVisitedObj != null) {
        operationalAreaVisitedCount = operationalAreaVisitedObj.getCount();
      }

      ColumnData operationalAreaVisitedColumnData = new ColumnData();
      operationalAreaVisitedColumnData.setValue(operationalAreaVisitedCount);
      operationalAreaVisitedColumnData.setIsPercentage(false);

      String personLocationBusinessStatusKey =
          planIdentifier + "_" + childLocation.getIdentifier() + "_" + plan.getLocationHierarchy()
              .getIdentifier();
      PersonBusinessStatus personLocationBusinessStatusObj = personBusinessStatus.get(personLocationBusinessStatusKey);

      double noOfTreatedStructures = 0;
      double noOfChildrenTreated = 0;
      double noOfPeopleEligible = 0;
      if (personLocationBusinessStatusObj != null) {
        noOfTreatedStructures = personLocationBusinessStatusObj.getLocationsTreated().size();
        noOfChildrenTreated = personLocationBusinessStatusObj.getPersonTreated().size();
        noOfPeopleEligible = personLocationBusinessStatusObj.getPersonEligible().size();
      }

      ColumnData noOfTreatedStructuresColumnData = new ColumnData();
      noOfTreatedStructuresColumnData.setValue(noOfTreatedStructures);
      noOfTreatedStructuresColumnData.setIsPercentage(false);

      double percentageOfTreatedStructuresToTotalStructures = totalStructureCount > 0 ? noOfTreatedStructures / totalStructureCount * 100 : 0;

      ColumnData percentageOfTreatedStructuresToTotalStructureColumnData = new ColumnData();
      percentageOfTreatedStructuresToTotalStructureColumnData.setValue(percentageOfTreatedStructuresToTotalStructures);
      percentageOfTreatedStructuresToTotalStructureColumnData.setIsPercentage(true);

      double percentageOfChildrenTreatedToPeopleEligible = noOfPeopleEligible > 0 ? noOfChildrenTreated / noOfPeopleEligible * 100 : 0;

      ColumnData percentageOfChildrenTreatedToPeopleEligibleColumnData = new ColumnData();
      percentageOfChildrenTreatedToPeopleEligibleColumnData.setValue(percentageOfChildrenTreatedToPeopleEligible);
      percentageOfChildrenTreatedToPeopleEligibleColumnData.setIsPercentage(true);

      Map<String, ColumnData> columns = new HashMap<>();
      columns.put("Total Structures", totalStructuresColumnData);
      columns.put("Total Structures Found", totalStructuresFoundColumnData);
      columns.put("Found Coverage", totalFoundCoverageColumnData);
      columns.put("Operational Area Visited", operationalAreaVisitedColumnData);
      columns.put("Total Structures Received SPAQ",noOfTreatedStructuresColumnData);
      columns.put("Distribution Coverage",percentageOfTreatedStructuresToTotalStructureColumnData);
      columns.put("Treatment coverage",percentageOfChildrenTreatedToPeopleEligibleColumnData);

      RowData rowData = new RowData();
      rowData.setLocationIdentifier(childLocation.getIdentifier());
      rowData.setColumnDataMap(columns);
      rowData.setLocationName(childLocation.getName());
      return rowData;
    }).collect(Collectors.toList());

    TableRow tableRow = new TableRow();
    tableRow.setRowData(rowDatas);
    tableRow.setPlanIdentifier(planIdentifier);
    tableRow.setReportIdentifier(reportIdentifier);
    tableRow.setParentLocationIdentifier(parentLocationIdentifier);

    return tableRow;
  }
}
