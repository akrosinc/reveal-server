package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
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

}
