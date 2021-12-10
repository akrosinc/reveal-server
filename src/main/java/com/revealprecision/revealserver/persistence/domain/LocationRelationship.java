package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
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
@SQLDelete(sql = "UPDATE location_relationship SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
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
