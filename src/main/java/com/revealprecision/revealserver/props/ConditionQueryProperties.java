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
      "SELECT tl.location_identifier as location_identifier, tgl.name as geographic_level_name from plan_locations tl"
          + "LEFT JOIN location t on t.identifier = tl.location_identifier"
          + "LEFT JOIN geographic_level tgl on tgl.identifier = t.geographic_level_identifier"
          + "WHERE tl.plan_identifier = \n";
}


