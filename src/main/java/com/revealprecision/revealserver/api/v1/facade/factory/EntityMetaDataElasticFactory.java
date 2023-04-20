package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.MetadataElastic;

public class EntityMetaDataElasticFactory {
  public static EntityMetadataElastic getEntityMetadataElastic(String eventAggregationNumericProjection,
      String eventAggregationNumericProjection1, Double eventAggregationNumericProjection2) {
    EntityMetadataElastic entityMetadataElasticSum = new EntityMetadataElastic();
    MetadataElastic metadataElasticSum = new MetadataElastic();
    metadataElasticSum.setPlanId(
        eventAggregationNumericProjection);
    entityMetadataElasticSum.setTag(
        eventAggregationNumericProjection1);
    entityMetadataElasticSum.setActive(true);
    entityMetadataElasticSum.setType("import");
    entityMetadataElasticSum.setValueNumber(
        eventAggregationNumericProjection2);
    entityMetadataElasticSum.setMeta(metadataElasticSum);
    return entityMetadataElasticSum;
  }
}
