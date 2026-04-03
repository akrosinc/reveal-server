package com.revealprecision.revealserver.persistence.projection;

public interface PopulationSummaryProjection {

    Long getTotalPopulation();

    Long getMalePopulation();

    Long getFemalePopulation();
}