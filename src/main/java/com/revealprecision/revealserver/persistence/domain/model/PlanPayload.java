package com.revealprecision.revealserver.persistence.domain.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlanPayload implements Serializable {
    private String identifier;
    private String title;
}
