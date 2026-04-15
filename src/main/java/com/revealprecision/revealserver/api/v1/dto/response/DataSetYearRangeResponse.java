package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.persistence.domain.Dataset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DataSetYearRangeResponse {
   private UUID datasetId;
   private Integer maxYear;
   private Integer minYear;
}
