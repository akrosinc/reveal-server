package com.revealprecision.revealserver.constants;

import java.util.Arrays;
import java.util.List;

public interface FormConstants {

  String CDD_SUPERVISOR_DAILY_SUMMARY_HEALTH_WORKER_SUPERVISOR_FIELD = "health_worker_supervisor";
  String CDD_SUPERVISOR_DAILY_SUMMARY_CDD_NAME_FIELD = "cdd_name";
  String CDD_SUPERVISOR_DAILY_SUMMARY_FORM = "cdd_supervisor_daily_summary";
  String CDD_SUPERVISOR_DAILY_SUMMARY_DATE_FIELD = "date";
  String TABLET_ACCOUNTABILITY_FORM = "tablet_accountability";
  String TABLET_ACCOUNTABILITY_LOCATION_FIELD = "location";
  String TABLET_ACCOUNTABILITY_HEALTH_WORKER_SUPERVISOR_FIELD = "health_worker_supervisor";
  String TABLET_ACCOUNTABILITY_CDD_NAME_FIELD = "cdd_name";
  String CDD_DRUG_ALLOCATION_FORM = "cdd_drug_allocation";
  String CDD_DRUG_ALLOCATION_DATE_FIELD = "date";
  String CDD_DRUG_ALLOCATION_LOCATION_FIELD = "location";
  String CDD_DRUG_ALLOCATION_HEALTH_WORKER_SUPERVISOR_FIELD = "health_worker_supervisor";
  String CDD_DRUG_ALLOCATION_CDD_NAME_FIELD = "cdd_name";
  String SPRAY_FORM = "Spray";
  String SPRAY_FORM_SPRAY_OPERATOR_FIELD = "sprayop_code";
  String SPRAY_FORM_PROVIDER = "providerId";
  String SPRAY_FORM_SACHET_COUNT_FIELD = "serial_numbers_count";
  String IRS_FOUND = "FOUND";
  String IRS_SPRAYED = "SPRAYED";
  String IRS_NOT_SPRAYED = "NOT SPRAYED";
  String IRS_ELIGIBLE = "ELIGIBLE";
  String IRS_SACHET_COUNT = "SACHET_COUNT";
  String IRS_LITE_VERIFICATION_FORM = "irs_lite_verification";
  String IRS_LITE_FOUND = "FOUND";
  String IRS_LITE_SPRAYED = "SPRAYED";
  String IRS_LITE_NOT_SPRAYED = "NOT SPRAYED";
  String IRS_LITE_ELIGIBLE = "ELIGIBLE";
  String IRS_LITE_VERIFICATION_FORM_SUPERVISOR = "supervisor";

  String SPRAYED_PREGWOMEN = "sprayed_pregwomen";
  String SPRAYED_MALES = "sprayed_males";
  String SPRAYED_FEMALES = "sprayed_females";
  String ROOMS_SPRAYED = "rooms_sprayed";
  String HOH_PHONE = "hoh_phone";
  String NOTSPRAYED_REASON = "notsprayed_reason";
  String STRUCTURE_SPRAYED = "structure_sprayed";
  String ELIGIBILITY = "eligibility";
  String YES = "yes";
  String ELIGIBLE = "eligible";
  String REGISTER_STRUCTURE = "Register_Structure";
  String IRS_SA_DECISION = "irs_sa_decision";
  String COLLECTION_DATE = "collection_date";
  String DAILY_SUMMARY = "daily_summary";
  String MOBILIZATION_DATE = "mobilization_date";
  String SPRAY_DATE = "sprayDate";
  String IRS_LITE_VERIFICATION = "irs_lite_verification";
  String MOBILIZED = "mobilized";
  String MOBILIZATION = "mobilization";
  String SPRAY = "Spray";
  String LOCATION_PARENT = "locationParent";
  String BOTTLES_EMPTY = "bottles_empty";

  String BUSINESS_STATUS = "business_status";

  String COMPOUNDHEADNAME = "compoundheadname";

  String NAME_HO_H = "nameHoH";

  interface BusinessStatus {

    String NOT_DISPENSED = "Not Dispensed";
    String NOT_VISITED = "Not Visited";
    String NOT_SPRAYED = "Not Sprayed";
    String SPRAYED = "Sprayed";
    String NOT_SPRAYABLE = "Not Sprayable";
    String COMPLETE = "Complete";
    String ALL_TASKS_COMPLETE = "All Tasks Complete";
    String INCOMPLETE = "Incomplete";
    String NOT_ELIGIBLE = "Not Eligible";
    String IN_PROGRESS = "In Progress";


    String FULLY_RECEIVED = "Fully Received";
    String NONE_RECEIVED = "None Received";
    String ADHERENCE_VISIT_DONE = "Adherence Visit Done";
    String PARTIALLY_RECEIVED = "Partially Received";

    String SMC_COMPLETE = "SMC Complete";
    String SPAQ_COMPLETE = "SPAQ Complete";
    String INELIGIBLE = "Ineligible";
    String TASKS_INCOMPLETE = "Tasks Incomplete";
    String FAMILY_NO_TASK_REGISTERED = "Family No Task Registered";

    String FAMILY_REGISTERED = "Family Registered";
    String BEDNET_DISTRIBUTED = "Bednet Distributed";
    String BLOOD_SCREENING_COMPLETE = "Blood Screening Complete";
    String PARTIALLY_SPRAYED = "Partially Sprayed";


    List<String> IRS_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, NOT_SPRAYED,
        SPRAYED, NOT_SPRAYABLE, COMPLETE, INCOMPLETE, NOT_ELIGIBLE, IN_PROGRESS);

    List<String> FI_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, FAMILY_REGISTERED,
        BEDNET_DISTRIBUTED,
        BLOOD_SCREENING_COMPLETE, COMPLETE, NOT_ELIGIBLE);

    List<String> MDA_LITE_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, IN_PROGRESS, COMPLETE);
    List<String> MDA_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, FULLY_RECEIVED, NONE_RECEIVED,
        ADHERENCE_VISIT_DONE, PARTIALLY_RECEIVED, COMPLETE, NOT_ELIGIBLE, NOT_VISITED, SMC_COMPLETE,
        INELIGIBLE,
        TASKS_INCOMPLETE, COMPLETE, NOT_ELIGIBLE, FAMILY_NO_TASK_REGISTERED, ALL_TASKS_COMPLETE,
        SPAQ_COMPLETE);
  }

  interface Colors {

    String grey = "#8B8B8B";
    String red = "#EE0427";
    String black = "#000000";

    String orange = "#ED8231";

    String green = "#6CBF0F";
    String yellow = "#FFCA16";
  }
}
