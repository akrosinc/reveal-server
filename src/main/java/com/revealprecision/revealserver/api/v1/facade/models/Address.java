package com.revealprecision.revealserver.api.v1.facade.models;

import java.util.Date;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {

  private String addressType;

  private Date startDate;

  private Date endDate;

  private Map<String, String> addressFields;

  private String latitude;

  private String longitude;

  private String geopoint;

  private String postalCode;

  private String subTown;

  private String town;

  private String subDistrict;

  private String countyDistrict;

  private String cityVillage;

  private String stateProvince;

  private String country;

}
