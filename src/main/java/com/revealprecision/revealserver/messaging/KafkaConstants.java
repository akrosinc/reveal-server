package com.revealprecision.revealserver.messaging;

public class KafkaConstants {

  public static final String PLAN_UPDATE = "PLAN_UPDATE";
  public static final String TASK = "TASK";
  public static final String PLAN_LOCATION_ASSIGNED = "PLAN_LOCATION_ASSIGNED";
  public static final String PLAN_LOCATION_ASSIGNED_STREAM = "PLAN_LOCATION_ASSIGNED_STREAM";
  public static final String LOCATIONS_IMPORTED = "LOCATIONS_IMPORTED";
  public static final String LOCATION_METADATA_UPDATE = "LOCATION_METADATA_UPDATE";
  public static final String PERSON_METADATA_UPDATE = "PERSON_METADATA_UPDATE";
  public static final String tableOfOperationalAreaHierarchiesTOPIC = "tableOfOperationalAreaHierarchiesTOPIC";
  public static final String TASK_CANDIDATE = "TASK_CANDIDATE";
  public static final String EVENT_CONSUMPTION = "EVENT_CONSUMPTION";
  public static final String FORM_EVENT_CONSUMPTION = "FORM_EVENT_CONSUMPTION";

  public static final String LOCATION_SUPERVISOR_CDD = "LOCATION_SUPERVISOR_CDD";

  ///Materialized stores
  public static final String structureCountPerParent = "structureCountPerParent";
  public static final String assignedStructureCountPerParent = "assignedStructureCountPerParent";
  public static final String tableOfAssignedStructuresWithParentKeyed = "tableOfAssignedStructuresWithParentKeyed";
  public static final String tableOfOperationalAreas = "tableOfOperationalAreas";
  public static final String tableOfOperationalAreaHierarchies = "tableOfOperationalAreaHierarchies";
  public static final String taskPlanParent = "taskPlanParent";
  public static final String taskParent = "taskParent";
  public static final String locationBusinessStatus = "locationBusinessStatus";
  public static final String locationBusinessStatusByPlanParentHierarchy = "locationBusinessStatusByPlanParentHierarchy";
  public static final String operationalAreaByPlanParentHierarchy = "operationalAreaByPlanParentHierarchy";
  public static final String locationBusinessStatusForOperationalAreas = "locationBusinessStatusForOperationalAreas";
  public static final String personBusinessStatus = "personBusinessStatus";
  public static final String structurePeople = "structurePeople";
  public static final String structurePeopleCounts = "structurePeopleCounts";
  public static final String hierarchicalPeopleTreatmentData = "hierarchicalPeopleTreatmentData";
  public static final String hierarchicalPeopleTreatmentCounts = "hierarchicalPeopleTreatmentCounts";
  public static final String operationalAreaTreatmentData = "operationalAreaTreatmentData";
  public static final String restructuredOperationalAreaTreatmentData = "restructuredOperationalAreaTreatmentData";
  public static final String tableOfOperationalAreaHierarchiesForPersonStream = "tableOfOperationalAreaHierarchiesForPersonStream";
  public static final String joinedOperationalAreaTreatmentData = "joinedOperationalAreaTreatmentData";
  public static final String operationalTreatedCounts = "operationalTreatedCounts";
  public static final String task = "task";
  public static final String assignedOperationalCountPerParent = "assignedOperationalCountPerParent";
  public static final String locationStructureBusinessStatus = "locationStructureBusinessStatus";
  public static final String locationStructureHierarchyBusinessStatus = "locationStructureHierarchyBusinessStatus";
  public static final String personFormDataString = "personFormDataString";
  public static final String personFormDataStringCount = "personFormDataStringCount";
  public static final String locationFormDataInteger = "locationFormDataInteger";
  public static final String locationFormDataIntegerSumOrAverage = "locationFormDataIntegerSumOrAverage";
  public static final String locationFormDataString = "locationFormDataString";
  public static final String locationFormDataStringCount = "locationFormDataStringCount";
  public static final String personFormDataInteger = "personFormDataInteger";
  public static final String personFormDataIntegerSumOrAverage = "personFormDataIntegerSumOrAverage";
  public static final String mdaLiteSupervisors = "mdaLiteSupervisors";
  public static final String cddNames = "cddNames";
  public static final String supervisorLocationFormDataIntegerSumOrAverage = "supervisorLocationFormDataIntegerSumOrAverage";
  public static final String cddSupervisorLocationFormDataIntegerSumOrAverage = "cddSupervisorLocationFormDataIntegerSumOrAverage";
}
