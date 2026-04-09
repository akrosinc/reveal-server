package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.DataSetEntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.DatasetResponse;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.projection.InstanceEntityTagIdProjection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DatasetImportResponseFactory {

  public static Page<DatasetResponse> fromEntityPage(
      Page<MetadataImport> metadataImportPage,
      Map<UUID, List<EntityTag>> entityTagsByMetadataId,
      List<InstanceEntityTagIdProjection> instances,
      Pageable pageable) {

    // Create a map of entityTagId to instance name for lookup
    Map<String, String> entityTagIdToInstanceName = mapEntityTagIdsToInstanceNames(instances);

    List<DatasetResponse> responses = metadataImportPage.getContent().stream()
        .map(metadataImport -> buildDatasetResponse(
            metadataImport,
            entityTagsByMetadataId,
            entityTagIdToInstanceName))
        .filter(datasetResponse -> CollectionUtils.isNotEmpty(datasetResponse.getDatasetEntityTags()))
        .collect(Collectors.toList());

    return new PageImpl<>(responses, pageable, responses.size());
  }

  private static DatasetResponse buildDatasetResponse(
      MetadataImport metadataImport,
      Map<UUID, List<EntityTag>> entityMap,
      Map<String, String> entityTagIdToInstanceName) {



    DatasetResponse datasetResponse = DatasetResponse.builder()
        .identifier(metadataImport.getIdentifier())
        .datasetName(metadataImport.getMetadataName())
        .uploadDatetime(metadataImport.getUploadedDatetime())
        .uploadedBy(metadataImport.getUploadedBy())
        .build();

    if (entityMap != null && entityMap.containsKey(metadataImport.getIdentifier())) {
      List<EntityTag> entityTags = entityMap.getOrDefault(
          metadataImport.getIdentifier(), Collections.emptyList());

      // Get instance names for this metadata import
      List<String> instanceNames = entityTags.stream()
          .map(EntityTag::getIdentifier)
          .map(UUID::toString)
          .map(entityTagIdToInstanceName::get)
          .filter(Objects::nonNull)
          .distinct()
          .collect(Collectors.toList());

      List<DataSetEntityTagResponse> collect = entityTags.stream()
          .map(entityTag -> mapEntityTagToEntityTagResponse(entityTag,
              instanceNames)).collect(Collectors.toList());

      datasetResponse.setDatasetEntityTags(collect);
    }

    return datasetResponse;
  }

  private static DataSetEntityTagResponse mapEntityTagToEntityTagResponse(EntityTag entityTag,
      List<String> instanceNames){
    return DataSetEntityTagResponse.builder()
        .identifier(entityTag.getIdentifier())
        .tag(entityTag.getTag())
        .instances(instanceNames)
        .isPublic(entityTag.isPublic())
        .build();
  }

  private static Map<String, String> mapEntityTagIdsToInstanceNames( List<InstanceEntityTagIdProjection> instances) {
    Map<String, String> mapping = new HashMap<>();
    for (InstanceEntityTagIdProjection instance : instances) {
      mapping.put(instance.getEntityTagIdentifier(), instance.getName());
    }
    return mapping;
  }
}
