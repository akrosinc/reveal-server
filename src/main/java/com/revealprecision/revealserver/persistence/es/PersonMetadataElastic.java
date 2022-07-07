package com.revealprecision.revealserver.persistence.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonMetadataElastic {

  private String type;
  private Object value;
  private MetaDataElastic metadata;
  private boolean isActive = true;

  public PersonMetadataElastic(MetadataObj metadataObj) {
    this.type = metadataObj.getDataType();
    this.isActive = metadataObj.isActive();

    switch (metadataObj.getDataType()){
      case "string":
        this.value = metadataObj.getCurrent().getValue().getValueString();
        break;
      case "integer":
        this.value = metadataObj.getCurrent().getValue().getValueInteger();
        break;
      case "double":
        this.value = metadataObj.getCurrent().getValue().getValueDouble();
        break;
      case "date":
        this.value = metadataObj.getCurrent().getValue().getValueDate();
        break;
      case "boolean":
        this.value = metadataObj.getCurrent().getValue().getValueBoolean();
        break;
    }
  }

  public PersonMetadataElastic(MetaDataEvent metadataObj) {
    this.type = metadataObj.getType();
    this.isActive = metadataObj.isActive();

    switch (metadataObj.getType()){
      case "string":
        this.value = metadataObj.getTagData().getValue().getValueString();
        break;
      case "integer":
        this.value = metadataObj.getTagData().getValue().getValueInteger();
        break;
      case "double":
        this.value = metadataObj.getTagData().getValue().getValueDouble();
        break;
      case "date":
        this.value = metadataObj.getTagData().getValue().getValueDate();
        break;
      case "boolean":
        this.value = metadataObj.getTagData().getValue().getValueBoolean();
        break;
    }
  }
}
