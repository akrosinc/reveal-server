package com.revealprecision.revealserver.persistence.domain.aggregation;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "mw_import_aggregate_numeric_by_date")
@Getter
@NoArgsConstructor
public class ImportAggregateNumericByDate {

    @Id
    private UUID id;

    private String name;

    @Column(name = "locationidentifier")
    private String locationidentifier;

    @Column(name = "hierarchyidentifier")
    private String hierarchyidentifier;

    @Column(name = "planidentifier")
    private String planidentifier;

    @Column(name = "eventtype")
    private String eventtype;

    @Column(name = "fieldcode")
    private String fieldcode;

    @Column(name = "datacapturedate")
    private LocalDate datacapturedate;

    @Column(name = "year")
    private Integer year;

    @Column(name = "sum")
    private Double sum;

    @Column(name = "avg")
    private Double avg;

    @Column(name = "median")
    private Double median;

    @Column(name = "min")
    private Double min;

    @Column(name = "max")
    private Double max;

    @Column(name = "count")
    private Long count;
}