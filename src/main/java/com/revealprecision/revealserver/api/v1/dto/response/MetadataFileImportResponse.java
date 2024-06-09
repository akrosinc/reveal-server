package com.revealprecision.revealserver.api.v1.dto.response;


import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.Owner;
import java.time.LocalDateTime;
import java.util.List;
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
public class MetadataFileImportResponse {

  private UUID identifier;
  private String filename;
  private LocalDateTime uploadDatetime;
  private BulkEntryStatus status;
  private String uploadedBy;

  private List<EntityTagEvent> entityTagEvents;

  private boolean isOwner;
  private List<Owner> owners;
  private boolean isAdmin;
}
