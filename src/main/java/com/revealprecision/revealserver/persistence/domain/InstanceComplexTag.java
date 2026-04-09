// InstanceComplexTag.java
package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceComplexTagId;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "instance_complex_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class InstanceComplexTag {

    @EmbeddedId
    private InstanceComplexTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instanceId")
    @JoinColumn(name = "instance_id")
    private Instance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("complexTagId")
    @JoinColumn(name = "complex_tag_id")
    private ComplexTag complexTag;

    public void populate(Instance instance, ComplexTag complexTag) {
        this.instance = instance;
        this.complexTag = complexTag;
    }
}