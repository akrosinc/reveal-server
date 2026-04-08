package com.revealprecision.revealserver.amdr.api.v1.dto.response;

import com.revealprecision.revealserver.amdr.model.AmdrImportStatus;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmrImportResultsResponse implements Serializable {

  private List<AmdrImportStatus> statuses;
}
