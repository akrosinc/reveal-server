package com.revealprecision.revealserver.api.v1.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.revealprecision.revealserver.api.v1.facade.models.ClientFacade;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventClientFacadeSyncResponse {

  private List<EventFacade> events;

  private List<ClientFacade> clients;

  @JsonProperty("no_of_events")
  private Integer noOfEvents;

  private String msg;

  @JsonProperty("total_records")
  private Long totalRecords;
}
