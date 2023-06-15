package com.revealprecision.revealserver.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.fhir.properties.FhirServerProperties;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.metadata.infra.MetadataObj;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PersonService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Person;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("FHIR")
public class PersonResourceProvider implements IResourceProvider {

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Person.class;
  }

  private final PersonService personService;

  private final FhirServerProperties fhirServerProperties;

  private final LocationService locationService;

  private final MetadataService metadataService;


  @Read()
  @Transactional
  public Person read(@IdParam IdType theId) {

    com.revealprecision.revealserver.persistence.domain.Person revealPerson = personService.getPersonByIdentifier(
        UUID.fromString(theId.getIdPart()));

    return getFhirPersonFromRevealPerson(revealPerson);
  }

  @Create
  public MethodOutcome create(@ResourceParam Person fhirPerson) {
    com.revealprecision.revealserver.persistence.domain.Person revealPersonFromFhirPerson = getRevealPersonFromFhirPerson(
        fhirPerson);
    revealPersonFromFhirPerson.setEntityStatus(EntityStatus.ACTIVE);
    Person person = getFhirPersonFromRevealPerson(
        personService.createPerson(revealPersonFromFhirPerson));
    MethodOutcome methodOutcome = new MethodOutcome();
    methodOutcome.setId(new IdType("Person", person.getId(), "1")).setResource(person);
    return methodOutcome;
  }

  private com.revealprecision.revealserver.persistence.domain.Person getRevealPersonFromFhirPerson(
      Person fhirPerson) {
    com.revealprecision.revealserver.persistence.domain.Person person = new com.revealprecision.revealserver.persistence.domain.Person();

    HumanName humanName = fhirPerson.getName().get(0);
    person.setNameGiven(humanName.getGiven().get(0).toString());
    person.setNameSuffix(humanName.getSuffix().get(0).toString());
    person.setNamePrefix(humanName.getPrefix().get(0).toString());
    person.setNameFamily(humanName.getFamily());
    person.setNameText(humanName.getText());
    person.setNameUse(humanName.getUse().toString());

    person.setGender(fhirPerson.getGender().toCode());
    person.setBirthDate(fhirPerson.getBirthDate());
    person.setActive(fhirPerson.getActive());

    return person;
  }

  private Person getFhirPersonFromRevealPerson(
      com.revealprecision.revealserver.persistence.domain.Person revealPerson) {

    Person person = new Person();
    Identifier identifier = new Identifier();
    identifier.setSystem("Reveal");
    identifier.setValue(revealPerson.getIdentifier().toString());

    person.setId(revealPerson.getIdentifier().toString());

    person.setIdentifier(List.of(identifier));
    person.setActiveElement(new BooleanType(revealPerson.isActive()));
    person.setActive(revealPerson.isActive());

    List<HumanName> humanNames = new ArrayList<>();
    HumanName humanName = new HumanName();
    humanName.addGiven(revealPerson.getNameGiven());
    humanName.addPrefix(revealPerson.getNamePrefix());
    humanName.addSuffix(revealPerson.getNameSuffix());
    humanName.setFamily(revealPerson.getNameFamily());
    humanName.setText(revealPerson.getNameText());

    humanName.setUse(NameUse.OFFICIAL);

    humanNames.add(humanName);
    person.setName(humanNames);

    person.setBirthDate(revealPerson.getBirthDate());
    person.setGender(AdministrativeGender.fromCode(revealPerson.getGender().toLowerCase()));
    person.setIdentifier(person.getIdentifier());

    Extension extension = person.addExtension().setUrl(
        fhirServerProperties.getBaseURL() + fhirServerProperties.getFhirPath()
            + "/CodeSystem/person-metadata");

    extension.setExtension(
        metadataService.getPersonMetadataByPerson(revealPerson.getIdentifier())
            .getEntityValue().getMetadataObjs().stream()
            .map(this::metadataExtension).collect(Collectors.toList()));

    revealPerson.getLocations().stream().map(this::getAddress).forEach(person::addAddress);

    return person;
  }

  private Address getAddress(Location location) {
    Address address = new Address();
    LocationCoordinatesProjection locationCoordinates = locationService.getLocationCentroidCoordinatesByIdentifier(
        location.getIdentifier());
    DecimalType latitude = new DecimalType();
    latitude.setValue(locationCoordinates.getLatitude());
    DecimalType longitude = new DecimalType();
    longitude.setValue(locationCoordinates.getLatitude());

    Attachment attachment = new Attachment();
    attachment.setContentType("application/geo+json");
    attachment.setData(location.getGeometry().toString().getBytes());

    Extension extension = address.addExtension()
        .setUrl("http://hl7.org/fhir/StructureDefinition/geolocation");
    extension.addExtension().setUrl("latitude").setValue(latitude);
    extension.addExtension().setUrl("longitude").setValue(longitude);
    extension.addExtension()
        .setUrl("http://build.fhir.org/extension-location-boundary-geojson.html").addExtension()
        .setUrl("geojson").setValue(attachment);
    return address;
  }

  private Extension metadataExtension(MetadataObj metadataObj) {

    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setText(String.valueOf(MetadataService.getValueFromValueObject(metadataObj).getSecond()));
    codeableConcept.setId(metadataObj.getTag());

    Extension extension = new Extension();
    extension.setUrl(metadataObj.getTag());
    extension.setValue(codeableConcept);

    return extension;
  }
}
