package com.revealprecision.revealserver.persistence.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Geometry implements Serializable {
    private String type;
    List<Double> coordinates;
}
