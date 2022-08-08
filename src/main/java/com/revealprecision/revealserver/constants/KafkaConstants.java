package com.revealprecision.revealserver.constants;

public interface KafkaConstants {

  String PLAN_UPDATE = "PLAN_UPDATE";
  String TASK = "TASK";
  String PLAN_LOCATION_ASSIGNED = "PLAN_LOCATION_ASSIGNED";
  String PLAN_LOCATION_ASSIGNED_STREAM = "PLAN_LOCATION_ASSIGNED_STREAM";
  String LOCATIONS_IMPORTED = "LOCATIONS_IMPORTED";
  String LOCATION_METADATA_UPDATE = "LOCATION_METADATA_UPDATE";
  String PERSON_METADATA_UPDATE = "PERSON_METADATA_UPDATE";
  String tableOfOperationalAreaHierarchiesTOPIC = "tableOfOperationalAreaHierarchiesTOPIC";
  String TASK_CANDIDATE = "TASK_CANDIDATE";
  String EVENT_CONSUMPTION = "EVENT_CONSUMPTION";
  String FORM_EVENT_CONSUMPTION = "FORM_EVENT_CONSUMPTION";
  String LOCATION_SUPERVISOR_CDD = "LOCATION_SUPERVISOR_CDD";
  String METADATA_AGGREGATE = "METADATA_AGGREGATE";
  String USER_DATA = "USER_DATA";
  String USER_PERFORMANCE_DATA = "USER_PERFORMANCE_DATA";
  String USER_PARENT_CHILD = "USER_PARENT_CHILD";

  String DISCOVERED_STRUCTURES = "DISCOVERED_STRUCTURES";

  String FORM_OBSERVATIONS = "FORM_OBSERVATIONS";
  String structureCountPerParent = "structureCountPerParent";
  String assignedStructureCountPerParent = "assignedStructureCountPerParent";
  String tableOfAssignedStructuresWithParentKeyed = "tableOfAssignedStructuresWithParentKeyed";
  String tableOfOperationalAreas = "tableOfOperationalAreas";
  String tableOfOperationalAreaHierarchies = "tableOfOperationalAreaHierarchies";
  String taskPlanParent = "taskPlanParent";
  String taskParent = "taskParent";
  String locationBusinessStatus = "locationBusinessStatus";
  String locationBusinessStatusByPlanParentHierarchy = "locationBusinessStatusByPlanParentHierarchy";
  String operationalAreaByPlanParentHierarchy = "operationalAreaByPlanParentHierarchy";
  String locationBusinessStatusForOperationalAreas = "locationBusinessStatusForOperationalAreas";
  String personBusinessStatus = "personBusinessStatus";
  String structurePeople = "structurePeople";
  String structurePeopleCounts = "structurePeopleCounts";
  String hierarchicalPeopleTreatmentData = "hierarchicalPeopleTreatmentData";
  String hierarchicalPeopleTreatmentCounts = "hierarchicalPeopleTreatmentCounts";
  String operationalAreaTreatmentData = "operationalAreaTreatmentData";
  String restructuredOperationalAreaTreatmentData = "restructuredOperationalAreaTreatmentData";
  String tableOfOperationalAreaHierarchiesForPersonStream = "tableOfOperationalAreaHierarchiesForPersonStream";
  String joinedOperationalAreaTreatmentData = "joinedOperationalAreaTreatmentData";
  String operationalTreatedCounts = "operationalTreatedCounts";
  String task = "task";
  String assignedOperationalCountPerParent = "assignedOperationalCountPerParent";
  String locationStructureBusinessStatus = "locationStructureBusinessStatus";
  String locationStructureHierarchyBusinessStatus = "locationStructureHierarchyBusinessStatus";
  String personFormDataString = "personFormDataString";
  String personFormDataStringCount = "personFormDataStringCount";
  String locationFormDataInteger = "locationFormDataInteger";
  String locationFormDataIntegerSumOrAverage = "locationFormDataIntegerSumOrAverage";
  String locationFormDataString = "locationFormDataString";
  String locationFormDataStringCount = "locationFormDataStringCount";
  String personFormDataInteger = "personFormDataInteger";
  String personFormDataIntegerSumOrAverage = "personFormDataIntegerSumOrAverage";
  String mdaLiteSupervisors = "mdaLiteSupervisors";
  String cddNames = "cddNames";
  String supervisorLocationFormDataIntegerSumOrAverage = "supervisorLocationFormDataIntegerSumOrAverage";
  String cddSupervisorLocationFormDataIntegerSumOrAverage = "cddSupervisorLocationFormDataIntegerSumOrAverage";
  String userPerformance = "userPerformance";
  String userPerformanceSums = "userPerformanceSums";
  String userParentChildren = "userParentChildren";
  String discoveredStructuresCountPerPlan = "discoveredStructuresCountPerPlan";
  String formObservations = "formObservations" ;
}
