package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;

public class MetadataEventFactory {

  public static MetaDataEvent getMetaDataEvent(MetadataObj metadataObj) {
    MetaDataEvent metaDataEvent = new MetaDataEvent();
    metaDataEvent.setTag(metadataObj.getTag());
    metaDataEvent.setTagData(metadataObj.getCurrent());
    metaDataEvent.setEntityTagId(metadataObj.getEntityTagId());
    metaDataEvent.setActive(metadataObj.isActive());
    metaDataEvent.setType(metadataObj.getType());
    metaDataEvent.setDataType(metadataObj.getDataType());
    metaDataEvent.setDateScope(metadataObj.isDateScope());
    metaDataEvent.setDateForDateScope(metadataObj.getDateForDateScope());
    metaDataEvent.setCaptureNumber(metadataObj.getCaptureNumber());
    return metaDataEvent;
  }

}
