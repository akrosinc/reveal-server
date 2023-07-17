package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SaveHierarchyPropertiesRequest {
    private String parent;
    private List<EntityMetadataResponse> metadata;
    List<String> ancestry;
    private Integer geographicLevelNumber;
}
