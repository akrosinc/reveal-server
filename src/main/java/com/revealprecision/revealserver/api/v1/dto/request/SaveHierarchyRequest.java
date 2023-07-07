package com.revealprecision.revealserver.api.v1.dto.request;

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
public class SaveHierarchyRequest {

    private String name;

    private DataFilterRequest submitSimulationRequestData;

    private List<SaveHierarchyLocationRequest> mapdata;
 }
