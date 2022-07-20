package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;


@Setter
@Getter
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventFacade extends BaseDataObject {

  @JsonProperty("_id")
  private String eventId;

  private Map<String, String> identifiers;

  private String baseEntityId;

  private String locationId;

  private String eventDate;

  private String eventType;

  private String formSubmissionId;

  private String providerId;

  private String status;

  private Map<String, String> statusHistory;

  private String priority;

  private List<String> episodeOfCare;

  private List<String> referrals;

  private String category;

  private int duration;

  private String reason;

  private List<Obs> obs;

  private String entityType;

  private Map<String, String> details;

  private long version;

  private List<Photo> photos;

  private String teamId;

  private String team;

  private String childLocationId;
}
