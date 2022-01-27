package com.revealprecision.revealserver.api.v1.dto.response;


import com.revealprecision.revealserver.enums.BulkStatusEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationBulkResponse {

  private UUID identifier;
  private String filename;
  private LocalDateTime uploadDatetime;
  private String uploadedBy;
  private BulkStatusEnum status;
}
