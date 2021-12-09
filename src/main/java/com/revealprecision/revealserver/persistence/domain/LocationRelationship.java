package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationRelationship extends AbstractAuditableEntity{
    @Id
    @GeneratedValue
    private UUID identifier;
    private UUID locationHierarchyIdentifier;
    private UUID locationIdentifier;
    private UUID parentIdentifier;

    @Type(type = "list-array")
    private List<UUID> ancestry;
}
