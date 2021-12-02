package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@NamedNativeQueries({
        @NamedNativeQuery(name = "Location.findByGeographicLevel",query = "select * from location where geographic_level_id = ?", resultClass = Location.class),
        @NamedNativeQuery(name = "Location.hasParentChildRelationship", query= "select  ST_Contains(?,?)")
})
public class Location extends AbstractAuditableEntity{
    @Id
    @GeneratedValue
    private UUID identifier;

    @ColumnDefault(value = "feature")
    private String type;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Geometry geometry;

    private String name;
    private String status;
    private UUID externalId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "geographic_level_id")
    private GeographicLevel geographicLevel;
}
