package com.revealprecision.revealserver.messaging;

public class KafkaConstants {

  public static final String version = "FORTYSEVEN";

  public static final String PLAN_UPDATE = "PLAN_UPDATE";
  public static final String TASK = "TASK";
  public static final String TASK_PARENT_PLAN = "TASK_PARENT_PLAN";
  public static final String PLAN_STRUCTURES_ASSIGNED = "PLAN_STRUCTURES_ASSIGNED";
  public static final String PLAN_STRUCTURES_COUNTS = "PLAN_STRUCTURES_COUNTS";
  public static final String PLAN_LOCATION_ASSIGNED = "PLAN_LOCATION_ASSIGNED";
  public static final String LOCATIONS_IMPORTED = "LOCATIONS_IMPORTED";
  public static final String LOCATION_METADATA_UPDATE = "LOCATION_METADATA_UPDATE";
  public static final String LOCATION_BUSINESS_STATUS_COUNTS = "LOCATION_BUSINESS_STATUS_COUNTS";
  public static final String OPERATIONAL_AREA_COUNTS = "OPERATIONAL_AREA_COUNTS";
  public static final String PERSON_METADATA_UPDATE = "PERSON_METADATA_UPDATE";


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
  public static final String personBusinessStatusByPlanParentHierarchy = "personBusinessStatusByPlanParentHierarchy";
}
