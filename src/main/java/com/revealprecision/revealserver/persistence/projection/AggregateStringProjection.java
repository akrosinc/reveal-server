package com.revealprecision.revealserver.persistence.projection;

public interface AggregateStringProjection extends AggregateProjection {

  String getFieldVal();

  Double getCount();

}
