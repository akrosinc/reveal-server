package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "condition.query")
@Component
public class ConditionQueryProperties {

  private String locationWithConditionsQuery =
      "SELECT a.identifier FROM location a \n"
          + " LEFT JOIN location_metadata am ON a.identifier = am.location_identifier \n"
          + " LEFT JOIN location_relationship lr on lr.location_identifier = a.identifier\n";

  private String personWithConditionQueryWithinALocationJurisdiction =
      "SELECT a.identifier FROM person a \n"
          + " LEFT JOIN person_metadata am ON a.identifier = am.person_identifier\n"
          + " LEFT JOIN person_location pl on a.identifier = pl.person_identifier\n"
          + " LEFT JOIN location l on l.identifier = pl.location_identifier \n"
          + " LEFT JOIN location_relationship lr on lr.location_identifier = l.identifier \n";

  private String locationWithoutConditionQuery =
      "SELECT a.identifier FROM location a \n"
          + " LEFT JOIN location_relationship lr on lr.location_identifier = a.identifier \n";

  private String planLocationsQuery =
      "SELECT location_identifier from plan_locations WHERE plan_identifier = \n";
}


