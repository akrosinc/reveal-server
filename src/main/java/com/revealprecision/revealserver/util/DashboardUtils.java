package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.api.v1.dto.models.ColumnData;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.persistence.domain.Plan;

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
}
