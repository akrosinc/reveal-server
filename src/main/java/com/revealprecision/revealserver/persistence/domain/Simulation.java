package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Simulation {

    @Id
    @GeneratedValue
    private UUID identifier;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_identifier", nullable = false, unique = true)
    @JsonBackReference
    private Plan plan;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "simulation_identifier",  nullable = false)
    @NotAudited
    private List<Dataset> datasets = new ArrayList<>();


    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "simulation_identifier", nullable = false)
    @NotAudited
    private List<RasterDataset> rasterDatasets = new ArrayList<>();

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
