package com.revealprecision.revealserver.persistence.es;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DATE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.util.ElasticModelUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityMetadataElastic {
//  @Field(type = FieldType.Keyword)
//  private String id;
  @Field(type = FieldType.Keyword)
  private String type;
  @Field(type = FieldType.Keyword)
  private String tag;
  @Field(type = FieldType.Keyword)
  private Object value;
  @Field(type = FieldType.Double)
  private Object valueNumber;
  @Field(type = FieldType.Flattened)
  private MetadataElastic meta;
  private boolean isActive = true;

  public EntityMetadataElastic(MetaDataEvent metadataObj) {
//    this.id = metadataObj.getEntityTagId().toString();
    this.type = metadataObj.getType();
    this.tag = metadataObj.getTag();
    this.isActive = metadataObj.isActive();
    this.meta = MetadataElastic.builder()
        .planId(metadataObj.getTagData().getMeta().getPlanId() == null ? null : metadataObj.getTagData().getMeta().getPlanId().toString())
        .userId(metadataObj.getTagData().getMeta().getUserId())
        .createDateTime(ElasticModelUtil.toDateFromLocalDateTime(metadataObj.getTagData().getMeta().getCreateDateTime()))
        .updateDateTime(ElasticModelUtil.toDateFromLocalDateTime(metadataObj.getTagData().getMeta().getUpdateDateTime()))
        .taskId(metadataObj.getTagData().getMeta().getTaskId()==null?null:metadataObj.getTagData().getMeta().getTaskId().toString())
        .taskType(metadataObj.getTagData().getMeta().getTaskType())
        .build();
    switch (metadataObj.getDataType()){
      case STRING:
        this.value = metadataObj.getTagData().getValue().getValueString();
        break;
      case INTEGER:
        this.valueNumber = metadataObj.getTagData().getValue().getValueInteger();
        break;
      case DOUBLE:
        this.valueNumber = metadataObj.getTagData().getValue().getValueDouble();
        break;
      case DATE:
        this.value = metadataObj.getTagData().getValue().getValueDate();
        break;
      case BOOLEAN:
        this.value = metadataObj.getTagData().getValue().getValueBoolean();
        break;
    }
  }
}
