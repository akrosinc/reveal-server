package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class LocationHierarchyWrapper {
    List<String> node_order;
}
