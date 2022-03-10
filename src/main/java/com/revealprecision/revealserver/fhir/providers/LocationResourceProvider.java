package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.api.v1.facade.service.MetaDataJdbcService;
import com.revealprecision.revealserver.fhir.properties.FhirServerProperties;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.service.LocationService;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationResourceProvider implements IResourceProvider {

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Location.class;
  }

  private final LocationService locationService;
  private final MetaDataJdbcService metaDataJdbcService;
  private final FhirServerProperties fhirServerProperties;

  @Read()
  @Transactional
  public Location read(@IdParam IdType theId) {

    com.revealprecision.revealserver.persistence.domain.Location location = locationService.findByIdentifier(
        UUID.fromString(theId.getIdPart()));

    return getFhirLocationFromRevealLocation(location);
  }

  public Location getFhirLocationFromRevealLocation(
      com.revealprecision.revealserver.persistence.domain.Location revealLocation) {

    Location location = new Location();
    location.setId(revealLocation.getIdentifier().toString());
    location.setDescription(revealLocation.getName());

    LocationCoordinatesProjection locationCoordinates = locationService.getLocationCentroidCoordinatesByIdentifier(
        revealLocation.getIdentifier());
    LocationPositionComponent locationPositionComponent = new LocationPositionComponent();
    locationPositionComponent.setLongitude(locationCoordinates.getLongitude());
    locationPositionComponent.setLatitude(locationCoordinates.getLatitude());

    location.setPosition(locationPositionComponent);
    location.setId(revealLocation.getIdentifier().toString());
    location.setDescription(revealLocation.getGeographicLevel().getName());
    Identifier identifier = new Identifier();
    identifier.setValue(revealLocation.getName());
    identifier.setUse(IdentifierUse.NULL);
    location.setIdentifier(List.of(identifier));

    Attachment attachment = new Attachment();
    attachment.setContentType("application/geo+json");
    attachment.setData(revealLocation.getGeometry().toString().getBytes());

    location.addExtension().setUrl("http://build.fhir.org/extension-location-boundary-geojson.html")
        .setValue(attachment);

    Extension extension = location.addExtension().setUrl(
        fhirServerProperties.getBaseURL() + fhirServerProperties.getFhirPath()
            + "/CodeSystem/location-metadata");

    extension.setExtension(
        metaDataJdbcService.getMetadataFor("location", revealLocation.getIdentifier()).entrySet()
            .stream().map(this::metadataExtension).collect(Collectors.toList()));

    return location;
  }

  private Extension metadataExtension(Entry<String, Pair<Class<?>, Object>> entrySet) {

    Class<?> aClass = entrySet.getValue().getKey();

    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setText(String.valueOf(aClass.cast(entrySet.getValue().getValue())));
    codeableConcept.setId(entrySet.getKey());

    Extension extension = new Extension();
    extension.setUrl(entrySet.getKey());
    extension.setValue(codeableConcept);

    return extension;
  }
}
