package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.fhir.properties.FhirServerProperties;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataService;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("FHIR")
public class LocationResourceProvider implements IResourceProvider {

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Location.class;
  }

  private final LocationService locationService;
  private final FhirServerProperties fhirServerProperties;
  private final MetadataService metadataService;

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


//    extension.setExtension(
//            metadataService.getLocationMetadataByLocation(revealLocation.getIdentifier())
//                .getEntityValue().getMetadataObjs().stream()
//            .map(this::metadataExtension).collect(Collectors.toList()));

    return location;
  }

//  private Extension metadataExtension(MetadataObj metadataObj) {
//
//    CodeableConcept codeableConcept = new CodeableConcept();
//    codeableConcept.setText(String.valueOf(MetadataService.getValueFromValueObject(metadataObj).getSecond()));
//    codeableConcept.setId(metadataObj.getTag());
//
//    Extension extension = new Extension();
//    extension.setUrl(metadataObj.getTag());
//    extension.setValue(codeableConcept);
//
//    return extension;
//  }
}
