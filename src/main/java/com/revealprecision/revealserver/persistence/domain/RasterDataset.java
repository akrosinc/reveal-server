package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RasterDataset {
    @Id
    @GeneratedValue
    private UUID identifier;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_layer_identifier")
    @JsonBackReference
    private MapLayer mapLayer;
    private String datasetIdentifier;
    private String name;
    private String colorRamp;
}