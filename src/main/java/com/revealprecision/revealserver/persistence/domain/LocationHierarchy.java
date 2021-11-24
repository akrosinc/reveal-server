package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
public class LocationHierarchy extends AbstractAuditableEntity {
    @Id
    @GeneratedValue
    private UUID identifier;
    @NotBlank(message = "order is required and must not be empty")
    private String nodes;
}
