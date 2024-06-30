package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationMetadataImportFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.MetadataImportResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.response.LocationMetadataImport;
import com.revealprecision.revealserver.api.v1.dto.response.MetadataFileImportResponse;
import com.revealprecision.revealserver.constants.EntityTagFieldTypes;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.BulkEntryStatus;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.MetadataImport;
import com.revealprecision.revealserver.persistence.domain.MetadataImportOwnership;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationString;
import com.revealprecision.revealserver.persistence.domain.metadata.SaveHierarchyMetadata;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.MetaImportDTO;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper.MetaFieldSetMapper;
import com.revealprecision.revealserver.persistence.domain.metadata.metadataImport.fieldMapper.MetaFieldSetMapper.ValidatedTagMap;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationNumericRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationStringRepository;
import com.revealprecision.revealserver.persistence.repository.MetadataImportRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.props.ImportAggregationProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final MetadataImportRepository metadataImportRepository;
  private final UserService userService;

  private final MetaFieldSetMapper metaFieldSetMapper;
  private final LocationRelationshipService locationRelationshipService;

  private final ImportAggregationNumericRepository importAggregationNumericRepository;
  private final ImportAggregationStringRepository importAggregationStringRepository;

  private final EntityTagService entityTagService;
  private final ImportAggregationProperties importAggregationProperties;
  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;

  @Transactional(rollbackOn = Exception.class)
  public ValidatedTagMap saveImportFile(String file, String fileName)
      throws FileFormatException {

    User currentUser = userService.getCurrentUser();

    MetadataImport metadataImport = new MetadataImport();
    metadataImport.setFilename(fileName);
    metadataImport.setEntityStatus(EntityStatus.ACTIVE);
    metadataImport.setUploadedDatetime(LocalDateTime.now());
    MetadataImportOwnership metadataImportOwnership = MetadataImportOwnership.builder()
        .metadataImport(metadataImport)
        .userSid(currentUser.getSid())
        .build();
    metadataImport.setOwners(List.of(metadataImportOwnership));

    Principal principal = UserUtils.getCurrentPrinciple();
    User user;
    UUID keycloakId = null;
    if (principal instanceof KeycloakPrincipal) {
      keycloakId = UUID.fromString(principal.getName());
    }
    user = userService.getByKeycloakId(keycloakId);
    metadataImport.setUploadedBy(user.getUsername());
    MetadataImport currentMetaImport = metadataImportRepository.save(metadataImport);

    try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
      XSSFSheet sheet = workbook.getSheetAt(0);

      XSSFRow tagNameRow = sheet.getRow(0);

      int cellCount = metaFieldSetMapper.getPhysicalNumberOfCells(tagNameRow);

      int fileRowsCount = metaFieldSetMapper.getFileRowsCount(sheet);

      Set<UUID> locationList = metaFieldSetMapper.extractIdsFor(sheet, fileRowsCount, 1,
          "Location");
      Set<UUID> hierarchyList = metaFieldSetMapper.extractIdsFor(sheet, fileRowsCount, 0,
          "Hierarchy");
      Set<String> geoLevels = metaFieldSetMapper.extractStringsFor(sheet, fileRowsCount, 3,
          "GeographicLevel");

      Map<UUID, Location> locationMap = metaFieldSetMapper.validateLocationsAndReturnLocationMapAndGetLocationMap(
          locationList);

      Map<UUID, LocationHierarchy> hierarchyMap = metaFieldSetMapper.validateHierarchiesAndReturnHierarchyMapAndGetHierarchyMap(
          hierarchyList);

      ValidatedTagMap validatedTagMap = metaFieldSetMapper.getTagsMap(
          sheet, currentMetaImport, tagNameRow, cellCount, currentUser,
          geoLevels.stream().findFirst().orElseThrow(
              () -> new FileFormatException("Geographic Levels passed in file is invalid")));

      metaFieldSetMapper.validateGeographicLevels(geoLevels, validatedTagMap);

      int rowCount = metaFieldSetMapper.getRowCount(sheet, fileRowsCount);

      List<MetaImportDTO> metaImportDTOS = metaFieldSetMapper.mapMetaFieldsDB(validatedTagMap,
          sheet,
          locationMap, hierarchyMap, rowCount);

      if (metaImportDTOS.stream().map(metaImportDTO -> metaImportDTO.getSheetData().getErrors())
          .map(Map::size).reduce(0, Integer::sum) > 1) {
        throw new FileFormatException("Invalid file errors with: " + metaImportDTOS.stream()
            .map(metaImportDTO -> metaImportDTO.getSheetData().getErrors())
            .flatMap(error -> error.entrySet().stream())
            .map(entry -> "tag: " + entry.getKey().getTag() + "value: " + entry.getValue())
            .collect(Collectors.joining("\r\n")));
      }
      Map<String, List<String>> ancestryMap = locationRelationshipService.getAncestryMap(
          metaImportDTOS.stream()
              .map(metaImportDTO -> metaImportDTO.getLocation().getIdentifier().toString())
              .collect(Collectors.toList()));

      currentMetaImport.setStatus(BulkEntryStatus.BUSY);
      //send data to kafka listener
      if (!metaImportDTOS.isEmpty()) {
        saveToDB(metaImportDTOS, ancestryMap, currentMetaImport);
        publishToMessagingGen(metaImportDTOS.stream().map(
                metaImportDTO -> new SaveHierarchyMetadata(
                    metaImportDTO.getLocation().getIdentifier().toString(),
                    metaImportDTO.getLocationHierarchy().getIdentifier().toString(),
                    metaImportDTO.getLocationHierarchy().getNodeOrder())).collect(Collectors.toList()),
            ancestryMap);
      }

      entityTagService.saveEntityTags(validatedTagMap.getEntityTags().stream()
          .peek(entityTag -> entityTag.setMetadataImport(currentMetaImport)).collect(
              Collectors.toSet()));

      return validatedTagMap;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new FileFormatException(e.getMessage());
    }

  }

  public void publishToMessagingGen(List<SaveHierarchyMetadata> saveHierarchyMetadatas,
      Map<String, List<String>> ancestryMap) {
    saveHierarchyMetadatas.forEach(saveHierarchyMetadata -> {

      log.trace("saveHierarchyMetadata: {}", saveHierarchyMetadata);
      if (ancestryMap.containsKey(saveHierarchyMetadata.getLocationIdentifier())) {
        ancestryMap.get(saveHierarchyMetadata.getLocationIdentifier())

            .forEach(ancestor -> publisherService.send(
                kafkaProperties.getTopicMap().get(KafkaConstants.AGGREGATION_STAGING),
                LocationIdEvent.builder()
                    .hierarchyIdentifier(saveHierarchyMetadata.getHierarchyIdentifier())
                    .nodeOrder(String.join(",", saveHierarchyMetadata.getNodeOrder()))
                    .uuids(List.of(UUID.fromString(ancestor)))
                    .build())
            );
      } else {
        log.error("location: {} does not have a location relationship", saveHierarchyMetadata);
      }

    });
  }


  void saveToDB(List<MetaImportDTO> metaImportDTOS,
      Map<String, List<String>> ancestryMap, MetadataImport currentMetaImport) {
    currentMetaImport.setStatus(BulkEntryStatus.BUSY);

    metaImportDTOS
        .stream().filter(metaImportDTO -> ancestryMap.containsKey(
            metaImportDTO.getLocation().getIdentifier().toString()))
        .forEach(metaImportDTO ->
            ancestryMap.get(metaImportDTO.getLocation().getIdentifier().toString())
                .forEach(ancestor ->
                    metaImportDTO.getSheetData().getConvertedEntityData().forEach(
                        (key, value) ->
                            updateDB(metaImportDTO.getLocation().getName(), ancestor, value,
                                key.getTag(), key.getValueType(),
                                metaImportDTO.getLocationHierarchy().getIdentifier().toString())
                    )));

    List<Entry<EntityTagEvent, Object>> string = metaImportDTOS.stream().flatMap(
            metaImportDTO -> metaImportDTO.getSheetData().getConvertedEntityData().entrySet().stream()
                .filter(entry -> entry.getKey().getValueType().equals(STRING) || entry.getKey()
                    .getValueType().equals(BOOLEAN)))
        .collect(Collectors.toList());

    Set<EntityTag> collect1 = getStringOrBooleanTagsGeneratedFromImportData(
        string);

    entityTagService.saveEntityTags(collect1);

    currentMetaImport.setStatus(BulkEntryStatus.SUCCESSFUL);
  }

  private Set<EntityTag> getStringOrBooleanTagsGeneratedFromImportData(
      List<Entry<EntityTagEvent, Object>> string) {
    Set<String> collect = string.stream()
        .flatMap(entityTagEventObjectEntry ->
            entityTagEventObjectEntry.getKey().getAggregationMethod().stream().map(method ->
                entityTagEventObjectEntry.getKey().getTag()
                    .concat(importAggregationProperties.getDelim())
                    .concat(String.valueOf(entityTagEventObjectEntry.getValue()))
                    .concat(importAggregationProperties.getDelim())
                    .concat(method))
        )
        .collect(Collectors.toSet());

    Map<String, EntityTag> collect2 = entityTagService.getEntityTagsByTagNames(
        collect).stream().collect(Collectors.toMap(EntityTag::getTag, a -> a, (a, b) -> b));

    return collect.stream().map(tag -> {
      EntityTag entityTag;
      if (collect2.containsKey(tag)) {
        entityTag = collect2.get(tag);
      } else {
        entityTag = EntityTag.builder()
            .isAggregate(true)
            .simulationDisplay(false)
            .valueType(DOUBLE)
            .tag(tag)
            .build();
      }
      return entityTag;
    }).collect(
        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(EntityTag::getTag))));

  }

  public void updateDB(String name, String locId, Object importEntityTagValue, String tag,
      String type, String hierarchyIdentifier) {
    try {
      switch (type) {
        case STRING:
        case BOOLEAN:
          Optional<ImportAggregationString> importAggregationStringOptional = importAggregationStringRepository.findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(
              name, locId, tag, hierarchyIdentifier);
          ImportAggregationString importAggregationString;
          if (importAggregationStringOptional.isPresent()) {
            importAggregationString = importAggregationStringOptional.get();
            importAggregationString.setVal((String) importEntityTagValue);
          } else {
            importAggregationString = ImportAggregationString.builder()
                .val((String) importEntityTagValue).ancestor(locId)
                .eventType(EntityTagFieldTypes.IMPORT)
                .fieldCode(tag)
                .hierarchyIdentifier(hierarchyIdentifier)
                .name(name).planIdentifier(null).build();
          }
          importAggregationStringRepository.save(importAggregationString);

          break;
        case DOUBLE:
        case INTEGER:
          Optional<ImportAggregationNumeric> importAggregationNumericOptional = importAggregationNumericRepository.findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(
              name, locId, tag, hierarchyIdentifier);
          ImportAggregationNumeric importAggregationNumeric;
          if (importAggregationNumericOptional.isPresent()) {
            importAggregationNumeric = importAggregationNumericOptional.get();
            importAggregationNumeric.setVal((Double) importEntityTagValue);
          } else {
            importAggregationNumeric = ImportAggregationNumeric.builder()
                .val((Double) importEntityTagValue).ancestor(locId)
                .hierarchyIdentifier(hierarchyIdentifier)
                .eventType(EntityTagFieldTypes.IMPORT).fieldCode(tag).name(name)
                .planIdentifier(null).build();
          }

          importAggregationNumericRepository.save(importAggregationNumeric);
          break;
      }
    } catch (Exception e) {
      //TODO: Need to handle import exceptions here and save them to the table
      log.error(e.getMessage(), e);
    }
  }

  public Page<MetadataFileImportResponse> getMetadataImportList(Pageable pageable) {

    User currentUser = userService.getCurrentUser();
    Page<MetadataImport> all = metadataImportRepository.findAll(pageable);

    Map<UUID, List<EntityTag>> collect = all.get().flatMap(
            metadataImport -> entityTagService.findEntityTagsByMetadataImport(
                    metadataImport.getIdentifier())
                .stream()
        ).filter(entityTag -> entityTag.getMetadataImport() != null)
        .collect(Collectors.groupingBy(entityTag -> entityTag.getMetadataImport().getIdentifier()));

    List<UUID> orgIds = collect.entrySet()
        .stream().flatMap(entityTagListEntry ->
            entityTagListEntry.getValue().stream()
                .flatMap((entityTag -> entityTag.getEntityTagAccGrantsOrganizations().stream().map(
                    EntityTagAccGrantsOrganization::getOrganizationId))
                )).collect(Collectors.toList());

    List<UUID> userIds = collect.entrySet()
        .stream().flatMap(entityTagListEntry ->
            entityTagListEntry.getValue().stream()
                .flatMap((entityTag -> entityTag.getEntityTagAccGrantsUsers().stream().map(
                    EntityTagAccGrantsUser::getUserSid))
                )).collect(Collectors.toList());

    List<UUID> ownerIds = collect.entrySet()
        .stream().flatMap(entityTagListEntry ->
            entityTagListEntry.getValue().stream()
                .flatMap((entityTag -> entityTag.getOwners().stream().map(
                    EntityTagOwnership::getUserSid))
                )).collect(Collectors.toList());

    ownerIds.addAll(
        all.getContent().stream().flatMap(metadataImport -> metadataImport.getOwners().stream().map(
            MetadataImportOwnership::getUserSid)).collect(
            Collectors.toList()));

    Set<Organization> orgGrants = organizationRepository.findByIdentifiers(orgIds);
    Set<User> userGrants = userRepository.findBySidIn(userIds);
    Set<User> owners = userRepository.findBySidIn(ownerIds);

    return MetadataImportResponseFactory.fromEntityPage(all, collect, orgGrants, userGrants,
        currentUser, owners,
        pageable);
  }

  public List<LocationMetadataImport> getMetadataImportDetails(UUID metaImportIdentifier) {
    Optional<MetadataImport> metadataImport = metadataImportRepository.findById(
        metaImportIdentifier);
    if (metadataImport.isPresent()) {
      List<LocationMetadataImport> locationMetadataImports = new ArrayList<>();
      metadataImport.get().getLocationMetadataEvents().forEach(el -> {
        //TODO: create a custom DTO for MetaDataEvent
        locationMetadataImports.add(LocationMetadataImportFactory.fromEntity(el));
      });
      return locationMetadataImports;
    } else {
      throw new NotFoundException("MetaImport not found.");
    }
  }


}

