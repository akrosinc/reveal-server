package com.revealprecision.revealserver.amdr.api.v1.dto.response;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmrImportResultsResponse implements Serializable {

  private int sampleIds;
}
