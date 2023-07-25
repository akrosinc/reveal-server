package com.revealprecision.revealserver.constants;

public interface EntityTagDataAggregationMethods {

  String DELIMITER = "-";

  String COUNT = "count";
  String MAX = "max";
  String MIN = "min";
  String AVERAGE = "average";
  String SUM = "sum";
  String MEDIAN = "median";

  String COUNT_ = DELIMITER + COUNT;
  String MAX_ = DELIMITER + MAX;
  String MIN_ = DELIMITER + MIN;
  String AVERAGE_ = DELIMITER + AVERAGE;
  String SUM_ = DELIMITER + SUM;
  String MEDIAN_ = DELIMITER + MEDIAN;
}
