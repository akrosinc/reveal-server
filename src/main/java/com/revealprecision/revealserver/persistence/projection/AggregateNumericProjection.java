package com.revealprecision.revealserver.persistence.projection;

public interface AggregateNumericProjection extends AggregateProjection {

  Double getSum();

  Double getAvg();

  Double getMedian();

  Double getMin();

  Double getMax();

}
