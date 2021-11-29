package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
public class LocationRelationship extends AbstractAuditableEntity{
    @Id
    @GeneratedValue
    private UUID identifier;
    private UUID location_hierarchy_identifier;
    private UUID location_identifier;
    private UUID parent_identifier;
    private String ancestry;


}
