package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Dataset {

    @Id
    @GeneratedValue
    private UUID identifier;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_tag_identifier")
    @JsonBackReference
    private EntityTag entityTag;

    private String name;

    private String hexColor;

    private Integer lineWidth;

    private String borderColor;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

}
