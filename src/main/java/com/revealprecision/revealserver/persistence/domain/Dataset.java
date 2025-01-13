package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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


}
