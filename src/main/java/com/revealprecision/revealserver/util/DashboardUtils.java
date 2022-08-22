package com.revealprecision.revealserver.util;

import static com.revealprecision.revealserver.constants.FormConstants.BusinessStatus.NOT_DISPENSED;
import static com.revealprecision.revealserver.constants.FormConstants.BusinessStatus.PARTIALLY_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.BusinessStatus.TASKS_INCOMPLETE;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.constants.FormConstants.BusinessStatus;
import com.revealprecision.revealserver.constants.FormConstants.Colors;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.persistence.domain.Plan;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashboardUtils {

  public static String getGeoNameDirectlyAboveStructure(Plan plan) {
    String geoNameDirectlyAboveStructure = null;
    if (plan.getLocationHierarchy().getNodeOrder().contains(LocationConstants.STRUCTURE)) {
      geoNameDirectlyAboveStructure = plan.getLocationHierarchy().getNodeOrder()
          .get(plan.getLocationHierarchy().getNodeOrder().indexOf(LocationConstants.STRUCTURE) - 1);
    }
    return geoNameDirectlyAboveStructure;
  }

  public static ColumnData getStringValueColumnData() {
    return ColumnData.builder().dataType("string").build();
  }

  public static String getBusinessStatusColor(String businessStatus) {
    switch (businessStatus) {
      case BusinessStatus.INELIGIBLE:
      case BusinessStatus.FAMILY_NO_TASK_REGISTERED:
        return Colors.grey;
      case BusinessStatus.NOT_SPRAYED:
      case NOT_DISPENSED:
      case BusinessStatus.IN_PROGRESS:
      case BusinessStatus.NONE_RECEIVED:
        return Colors.red;
      case BusinessStatus.SPRAYED:
      case BusinessStatus.SMC_COMPLETE:
      case BusinessStatus.SPAQ_COMPLETE:
      case BusinessStatus.ALL_TASKS_COMPLETE:
      case BusinessStatus.COMPLETE:
      case BusinessStatus.FULLY_RECEIVED:
      case PARTIALLY_SPRAYED:
        return Colors.green;
      case BusinessStatus.NOT_SPRAYABLE:
      case BusinessStatus.NOT_ELIGIBLE:
        return Colors.black;
      case BusinessStatus.INCOMPLETE:
      case TASKS_INCOMPLETE:
      case BusinessStatus.PARTIALLY_RECEIVED:
        return Colors.orange;
      default:
        log.debug(String.format("business status : %s is not defined :", businessStatus));
        return null;
    }
  }
}
