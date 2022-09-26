package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.FormConstants.BUSINESS_STATUS;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_DATE_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_DRUG_ALLOCATION_LOCATION_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_DATE_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.CDD_SUPERVISOR_DAILY_SUMMARY_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.COLLECTION_DATE;
import static com.revealprecision.revealserver.constants.FormConstants.DAILY_SUMMARY;
import static com.revealprecision.revealserver.constants.FormConstants.FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_ELIGIBLE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_FORM_SUPERVISOR;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_FOUND_FROM_SUMMARY;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_DAILY_SUMMARY_DISTRICT_MANAGER;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_DAILY_SUMMARY_LOCATION_ZONE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_DAILY_SUMMARY_MOPUP_MAIN;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_DAILY_SUMMARY_SPRAY_AREAS;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_ELIGIBLE;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_FOUND;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_NOT_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_LITE_VERIFICATION_FORM_SUPERVISOR;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_NOT_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_NOT_SPRAYED_OTHER;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_NOT_SPRAYED_REFUSED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SACHET_COUNT;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.IRS_SPRAYED_FROM_SUMMARY;
import static com.revealprecision.revealserver.constants.FormConstants.LOCATION_ID;
import static com.revealprecision.revealserver.constants.FormConstants.NOTSPRAYED_REASON;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAYED;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM_SACHET_COUNT_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.SPRAY_FORM_SPRAY_OPERATOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_CDD_NAME_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_FORM;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_HEALTH_WORKER_SUPERVISOR_FIELD;
import static com.revealprecision.revealserver.constants.FormConstants.TABLET_ACCOUNTABILITY_LOCATION_FIELD;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.FormDataEntityTagValueEventFactory;
import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.DeviceUser;
import com.revealprecision.revealserver.messaging.message.FormCaptureEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagEvent;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.OrgLevel;
import com.revealprecision.revealserver.messaging.message.UserData;
import com.revealprecision.revealserver.messaging.message.mdalite.MDALiteLocationSupervisorCddEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.projection.LocationAndHigherParentProjection;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.util.FormDataUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormDataProcessorService {


  private final ObjectMapper objectMapper;
  private final KafkaProperties kafkaProperties;
  private final PlanService planService;
  private final UserService userService;
  private final KafkaTemplate<String, FormDataEntityTagEvent> eventConsumptionTemplate;


  private final FormFieldService formFieldService;
  private final EntityTagService entityTagService;
  private final LocationService locationService;
  private final EventService eventService;

  private final KafkaTemplate<String, UserData> userDataTemplate;
  private final KafkaTemplate<String, MDALiteLocationSupervisorCddEvent> mdaliteSupervisorTemplate;

  private final KafkaTemplate<String, FormCaptureEvent> formSubmissionKafkaTemplate;
  private final LocationRelationshipService locationRelationshipService;







  private List<OrgLevel> getFlattenedOrganizationalHierarchy(Organization organization) {
    int levelCounter = 0;
    List<OrgLevel> orgHierarchy = new ArrayList<>();
    orgHierarchy.add(new OrgLevel(organization.getIdentifier().toString(), organization.getName(),
        levelCounter));
    Organization loopOrg = organization;

    while (loopOrg.getParent() != null) {
      loopOrg = loopOrg.getParent();
      orgHierarchy.add(
          new OrgLevel(loopOrg.getIdentifier().toString(), loopOrg.getName(), levelCounter));
      levelCounter++;
    }
    return orgHierarchy;
  }



  public void processFormDataAndSubmitToMessagingTemp(Event savedEvent)
      throws IOException {

    JsonNode obsList = savedEvent.getAdditionalInformation().get("obs");

    if (obsList.isArray()) {

      Plan plan = planService.getPlanByIdentifier(savedEvent.getPlanIdentifier());

      ObjectReader reader = objectMapper.readerFor(new TypeReference<List<Obs>>() {
      });

      List<Obs> obsJavaList = reader.readValue(obsList);

      if (plan != null) {

        User deviceUser = savedEvent.getUser();
        String fieldWorker = null;
        String fieldWorkerLabel = null;
        String district = null;
        String districtLabel = null;
        String userLabel = null;
        String orgLabel = null;
        boolean sprayed = false;
        boolean notSprayed = false;
        boolean isEligible = true;
        boolean notSprayedRefused = false;
        boolean notSprayedOther = false;
        UUID locationIdentifier = null;
        String submissionId = null;
        List<List<OrgLevel>> collect = new ArrayList<>();
        Map<String, Object> fields = new HashMap<>();
        Integer summarySprayed = null;
        Integer summaryFound = null;
        LocalDateTime captureDatetime = savedEvent.getCaptureDatetime();
        String deviceUserString = getFormValue(obsJavaList, IRS_FORM_SUPERVISOR);

        if (deviceUserString != null) {
          if (savedEvent.getEventType().equals(SPRAY_FORM)) {
            fieldWorker = getFormValue(obsJavaList, SPRAY_FORM_SPRAY_OPERATOR_FIELD);

            locationIdentifier = savedEvent.getLocationIdentifier();

            try {
              User user = userService.getByUserName(deviceUserString.split("\\|")[0].split(":")[0]);
              if (user != null) {
                deviceUser = user;
              }
            } catch (NotFoundException notFoundException) {
              log.warn("Supervisor {} not found in Reveal", deviceUserString);
            }

            String businessStatus = getFormValue(obsJavaList, BUSINESS_STATUS);

            if (businessStatus.equals("Complete")) {
              sprayed = true;
            }
            if (businessStatus.equals("Not Eligible")) {
              isEligible = false;
            }
            if (businessStatus.equals("Not Sprayed")) {
              notSprayed = true;
            }
            boolean found = true;

            String sachetCount = getFormValue(obsJavaList, SPRAY_FORM_SACHET_COUNT_FIELD);
            Integer sachetCountInt = 0;
            try {
              sachetCountInt = Integer.parseInt(sachetCount);
            } catch (ClassCastException e) {
              log.warn("Could not cast form data: {}, of value: {}", SPRAY_FORM_SACHET_COUNT_FIELD,
                  sachetCount);
            }

            String notSprayedReason = getFormValue(obsJavaList, NOTSPRAYED_REASON);
            if (notSprayedReason != null) {
              if (notSprayedReason.equals("Refused")) {
                notSprayedRefused = true;
              } else if (!notSprayedReason.trim().equals("") && !notSprayedReason.isEmpty()) {
                notSprayedOther = true;
              }
            }
            fields.put(IRS_FOUND, found);
            fields.put(IRS_SPRAYED, sprayed);
            fields.put(IRS_NOT_SPRAYED, notSprayed);
            fields.put(IRS_ELIGIBLE, isEligible);
            fields.put(IRS_SACHET_COUNT, sachetCountInt);
            fields.put(IRS_NOT_SPRAYED_REFUSED, notSprayedRefused);
            fields.put(IRS_NOT_SPRAYED_OTHER, notSprayedOther);

            submissionId = savedEvent.getTaskIdentifier().toString();

            LocationAndHigherParentProjection locationWithParent = locationRelationshipService.getHigherLocationParentByLocationAndParentGeographicLevelType(
                locationIdentifier, plan.getLocationHierarchy().getIdentifier(),
                LocationConstants.DISTRICT);

            district = locationWithParent.getHigherLocationParentName();
            districtLabel = "district";

            collect = deviceUser.getOrganizations().stream()
                .map(this::getFlattenedOrganizationalHierarchy).collect(Collectors.toList());

            fieldWorkerLabel = "Spray Operator";
            userLabel = "Supervisor";
            orgLabel = "Team";
          }
        }
        if (savedEvent.getEventType().equals(IRS_LITE_VERIFICATION_FORM)) {
          fieldWorker = getFormValue(obsJavaList, IRS_LITE_VERIFICATION_FORM_SUPERVISOR);

          String businessStatus = getFormValue(obsJavaList,
              BUSINESS_STATUS);

          if (businessStatus.equals("Sprayed")) {
            sprayed = true;
          }
          if (businessStatus.equals("Not Sprayed")) {
            notSprayed = true;
          }
          boolean found = true;

          fields.put(IRS_LITE_FOUND, found);
          fields.put(IRS_LITE_SPRAYED, sprayed);
          fields.put(IRS_LITE_NOT_SPRAYED, notSprayed);
          fields.put(IRS_LITE_ELIGIBLE, isEligible);

          collect = deviceUser.getOrganizations().stream()
              .map(this::getFlattenedOrganizationalHierarchy).collect(Collectors.toList());

          fieldWorkerLabel = "Supervisor";
          userLabel = "Field Officer";
          orgLabel = "Team";
        }
        if (deviceUserString !=null) {
          if (savedEvent.getEventType().equals(DAILY_SUMMARY)) {
            fieldWorker = getFormValue(obsJavaList, SPRAY_FORM_SPRAY_OPERATOR_FIELD);

            String collectionDate = getFormValue(obsJavaList, COLLECTION_DATE);
            captureDatetime = LocalDate.parse(collectionDate,
                DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();

            try {
              User user = userService.getByUserName(deviceUserString.split("\\|")[0]);
              if (user != null) {
                deviceUser = user;
              }
            } catch (NotFoundException notFoundException) {
              log.warn("Supervisor {} not found in Reveal", deviceUserString);
            }

            locationIdentifier = savedEvent.getLocationIdentifier();

            LocationAndHigherParentProjection locationWithParent = locationRelationshipService.getHigherLocationParentByLocationAndParentGeographicLevelType(
                locationIdentifier, plan.getLocationHierarchy().getIdentifier(),
                LocationConstants.DISTRICT);

            district = locationWithParent.getHigherLocationParentName();
            districtLabel = "district";

            submissionId =
                plan.getIdentifier() + "_" + collectionDate + "_" + deviceUserString + "_"
                    + fieldWorker;

            String foundSummary = getFormValue(obsJavaList, FOUND);

            String sprayedSummary = getFormValue(obsJavaList, SPRAYED);

            summarySprayed = Integer.parseInt(sprayedSummary);
            summaryFound = Integer.parseInt(foundSummary);

            fields.put(IRS_FOUND_FROM_SUMMARY, summaryFound);
            fields.put(IRS_SPRAYED_FROM_SUMMARY, summarySprayed);

            collect = deviceUser.getOrganizations().stream()
                .map(this::getFlattenedOrganizationalHierarchy).collect(Collectors.toList());

            fieldWorkerLabel = "Supervisor";
            userLabel = "Field Officer";
            orgLabel = "Team";

          }
        }

        userDataTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.USER_DATA),
            new UserData(submissionId, savedEvent.getPlanIdentifier(),
                new DeviceUser(deviceUser.getIdentifier(), deviceUser.getUsername()), userLabel,
                fieldWorker, fieldWorkerLabel, district, districtLabel, captureDatetime, collect,
                orgLabel,
                fields));

      }
    }
  }


  private UUID getBaseEntityIdentifierFromLocationFormData(List<Obs> obsJavaList,
      String formfield) {
    String locationName = getFormValue(obsJavaList, formfield);

    if (locationName != null) {
      List<Location> locations = locationService.getAllByNames(List.of(locationName));
      return locations.get(0).getIdentifier();
    }
    return null;
  }

  private String getFormValue(List<Obs> obsJavaList, String key) {
    Optional<Obs> ob = obsJavaList.stream()
        .filter(obs -> obs.getFieldCode().equals(key)).findFirst();
    return ob.map(obs -> (String) FormDataUtil.extractData(obs).get(obs.getFieldCode()))
        .orElse(null);
  }

  private String getFormValueFromList(List<Obs> obsJavaList, String key) {
    Optional<Obs> ob = obsJavaList.stream()
        .filter(obs -> obs.getFieldCode().equals(key)).findFirst();
    return ob.map(obs -> (String) FormDataUtil.extractDataFromList(obs).get(obs.getFieldCode()))
        .orElse(null);
  }
}
