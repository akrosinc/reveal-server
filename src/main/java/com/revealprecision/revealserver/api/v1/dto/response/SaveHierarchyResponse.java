package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SaveHierarchyResponse {

    private int identifier;

    private String name;

    private List<String> nodeOrder;

 }
