package com.revealprecision.revealserver.messaging;

public class KafkaConstants {

  public static final String version = "FORTYSEVEN";

  public static final String PLAN_UPDATE = "PLAN_UPDATE";
  public static final String TASK = "TASK";
  public static final String TASK_PARENT_PLAN = "TASK_PARENT_PLAN";
  public static final String TASK_PLAN = "TASK_PLAN";
  public static final String PLAN_STRUCTURES_ASSIGNED  = "PLAN_STRUCTURES_ASSIGNED";
  public static final String PLAN_LOCATION_ASSIGNED = "PLAN_LOCATION_ASSIGNED";
  public static final String STRUCTURES_IMPORTED  = "STRUCTURES_IMPORTED";
  public static final String STRUCTURES_IMPORTED_FOR_TASK  = "STRUCTURES_IMPORTED_FOR_TASK";
  public static final String HIERARCHY_STRUCTURES_IMPORTED  = "HIERARCHY_STRUCTURES_IMPORTED";
  public static final String HIERARCHY_STRUCTURES_ASSIGNED  = "HIERARCHY_STRUCTURES_ASSIGNED";
  public static final String LOCATIONS_IMPORTED  = "LOCATIONS_IMPORTED";
  public static final String JOINED_STRUCTURE_TABLE = "JOINED_STRUCTURE_TABLE";
  public static final String LOCATION_METADATA_UPDATE = "LOCATION_METADATA_UPDATE";
  public static final String PLAN_LOCATION_HIERARCHY_PARENT = "PLAN_LOCATION_HIERARCHY_PARENT";



  ///Materialized stores
  public static final String structureCountPerParent = "structureCountPerParent";
  public static final String assignedStructureCountPerParent = "assignedStructureCountPerParent";
  public static final String tableOfAssignedStructuresWithParentKeyed = "tableOfAssignedStructuresWithParentKeyed";
  public static final String taskPlanParent = "taskPlanParent";
  public static final String taskParent = "taskParent";
  public static final String locationBusinessStatus = "locationBusinessStatus";
  public static final String locationBusinessStatusByPlanParentHierarchy = "locationBusinessStatusByPlanParentHierarchy";

}
