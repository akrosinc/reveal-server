package com.revealprecision.revealserver.amdr.api.v1.dto.response;


import com.revealprecision.revealserver.amdr.persistence.domain.AmdrProcessingStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AmdrImportResponse {

  private UUID identifier;
  private String filename;
  private LocalDateTime uploadDatetime;
  private AmdrProcessingStatus status;
  private String uploadedBy;

}
