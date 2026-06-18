package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.response.DataExtractQueryResponse;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.DataExtractQuery;
import com.revealprecision.revealserver.persistence.repository.DataExtractQueryRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.csveed.row.RowWriter;
import org.csveed.row.RowWriterImpl;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExtractService {

  private final JdbcTemplate jdbcTemplate;

  private final DataExtractQueryRepository dataExtractQueryRepository;

  private final ObjectMapper objectMapper;

  public InputStreamResource extract(UUID planIdentifier, String queryLabel) throws IOException {

    List<DataExtractQuery> firstByPlanIdentifier = dataExtractQueryRepository.findByPlanIdentifierAndQueryLabel(
        planIdentifier, queryLabel);

    if (firstByPlanIdentifier == null || firstByPlanIdentifier.size() == 0) {
      throw new NotFoundException("No Query found for plan " + planIdentifier);
    }
    DataExtractQuery target = firstByPlanIdentifier.get(0);

    if (!target.getQuery().isEmpty()) {

      DbDataObj processedData;
      if (target.isCustom()){
        processedData = getDataFromDBCustom(planIdentifier,  target);
      } else {
        processedData = getDataFromDB(planIdentifier,  target);
      }

      processHeader(processedData);

      InputStream targetStream = processData(processedData);
      return new InputStreamResource(targetStream);
    }
    throw new NotFoundException("No Query found for plan " + planIdentifier);
  }

  private void processHeader(DbDataObj processedData) {
    if (processedData.getMaxCol()!=null && processedData.getMaxCol() >  processedData.getHeader().size()){
      int diff = processedData.getMaxCol() - processedData.getHeader().size();
      for (int i=0;i<diff;i++){
        processedData.getHeader().add("extra");
      }
    }
  }


  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DbDataObj{
    List<String> header;
    List<String[]> data;
    Integer maxCol = null;
  }

  private InputStream processData(DbDataObj data) throws IOException {
    StringWriter stringWriter = new StringWriter();
    RowWriter rowWriter = new RowWriterImpl(stringWriter);

    rowWriter.writeHeader(data.getHeader().toArray(new String[0]));

    log.debug("data size {}", data.getData().size());
    for (String[] row : data.getData()) {

      List<String> strArr = new ArrayList<>(Arrays.asList(row));
      log.debug("row size {}",strArr.size());

      rowWriter.writeRow(strArr.toArray(new String[0]));
    }
    stringWriter.close();
    return new ByteArrayInputStream(stringWriter.toString().getBytes());
  }

  private DbDataObj getDataFromDBCustom(UUID planIdentifier,
      DataExtractQuery target) {

    List<String> header = new ArrayList<>();

    List<DbRow> rows = jdbcTemplate.query(
        target.getQuery(),
        ps -> ps.setString(1, "%" + planIdentifier + "%"),
        (rs, rowNum) -> {

          String simpleColsJson = rs.getString("simpleCols");
          String repeatingColsJson = rs.getString("repeatingCols");
          String checkboxColsJson = rs.getString("checkboxCols");

          try {
            DbRow identifier = new DbRow(
                rs.getString("identifier"),
                objectMapper.readValue(simpleColsJson, SimpleCols.class),
                objectMapper.readValue(repeatingColsJson, RepeatingCols.class),
                objectMapper.readValue(checkboxColsJson, CheckboxCols.class)
            );
            return identifier;

          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        });

    List<DbRow> rowData = rows.stream().filter(Objects::nonNull).collect(Collectors.toList());

    DbDataObj object = processDbRowData(rowData);

    return object;
  }

  private DbDataObj processDbRowData(List<DbRow> rows) {

    List<String> FIXED_COLUMNS = List.of(
        "eligible_structure",
        "supervisor",
        "household_id",
        "household_id_value",
        "date",
        "eligible_person",
        "respondent_consent",
        "roof_material",
        "other_roof_material",
        "wall_material",
        "other_wall_material",
        "floor_material",
        "other_floor_material",
        "eaves",
        "any_windows",
        "window_type",
        "calculated_total_people",
        "children_5_to_17",
        "adults",
        "total_people",
        "hoh_m_or_f",
        "hoh_age",
        "hoh_attend_school",
        "hoh_highest_edu",
        "structures_separate",
        "structures_sleeping",
        "separate_rooms_sleeping",
        "structure_room_no_sleep",
        "type_of_structure_room_no_sleep",
        "structure_room_no_sleep_other",
        "sleeping_last_night",
        "anyone_sleep_outside",
        "sleeping_outside_last_night",
        "sleep_outside_period",
        "outside_sleep_location_have_cover",
        "outside_sleep_location",
        "have_any_mosquito_net",
        "anyone_use_mosquito_net",
        "under_five_under_net",
        "school_age_under_net",
        "adults_under_net",
        "sprayed_walls",
        "sprayed_months_ago",
        "mosquito_coils",
        "mosquito_coils_last_used",
        "topical_mosquito_repellent",
        "topical_mosquito_repellent_last_used",
        "insecticide_spray",
        "insecticide_spray_last_used",
        "money_used",
        "building_sleep_time_under_five",
        "building_sleep_time_school_age",
        "building_sleep_time_adult_women",
        "building_sleep_time_adult_men",
        "under_net_time_under_five",
        "under_net_time_school_age",
        "under_net_time_adult_women",
        "under_net_time_adult_men",
        "under_net_time_under_five_morning",
        "under_net_time_school_age_morning",
        "under_net_time_adult_women_morning",
        "under_net_time_adult_men_morning",
        "go_out_building_under_five_morning",
        "go_out_building_school_age_morning",
        "go_out_building_adult_women_morning",
        "go_out_building_adult_men_morning",
        "spend_time_outside_compound",
        "spend_time_outside_compound_selection",
        "spend_time_outside_compound_selection_other",
        "spend_time_outside_compound_morning",
        "spend_time_outside_compound_morning_selection",
        "spend_time_outside_compound_morning_other",
        "travel_to_another_location",
        "travel_to_another_location_frequency",
        "stay_to_another_location_period",
        "adults_another_location",
        "adults_another_location_other",
        "children_another_location",
        "children_another_location_other",
        "another_location_mosquito_bite_prevention",
        "another_location_mosquito_bite_prevention_other",
        "bed_nets_another_location",
        "who_uses_bed_nets_another_location",
        "who_uses_bed_nets_another_location_other",
        "have_comments",
        "comments",
        "business_status"
    );

    List<String> REPEATING_COLUMNS = List.of(
        "child_name",
        "know_dob",
        "dob",
        "less_than_one_year",
        "estimated_age",
        "estimated_age_months",
        "child_present",
        "child_severely_ill",
        "prepared_rdt",
        "sleep_under_net",
        "received_malaria_vaccine",
        "malaria_vaccine_dose",
        "card_review",
        "malaria_vaccine_in_card",
        "additional_people_name",
        "additional_individual_know_dob",
        "additional_individual_dob",
        "additional_individual_estimated_age",
        "additional_individual_child_severely_ill",
        "additional_individual_calculated_age",
        "additional_individual_prepared_rdt",
        "mosquito_net_type",
        "other_mosquito_net_type",
        "rdt_result",
        "al_given",
        "al_not_given_reason",
        "al_not_given_reason_other"
    );

    List<String> CHECKBOX_COLUMNS = List.of(
        "type_of_structure_room_no_sleep"
        ,"spend_time_outside_compound_selection"
        ,"spend_time_outside_compound_morning_selection"
        ,"another_location_mosquito_bite_prevention"
    );

    List<String> reportColumns = new ArrayList<>(FIXED_COLUMNS);

    List<String> repeatingColumns = new ArrayList<>(REPEATING_COLUMNS);

    List<String> checkboxColumns = new ArrayList<>(CHECKBOX_COLUMNS);

    DbDataObj dbDataObj = new DbDataObj();
    List<String[]> cols = new ArrayList<>();
    int maxCol = 0;
    for (DbRow dbRow: rows) {
      List<String> combined = new ArrayList<>();
      List<String> simpleCols = getProcessSimpleCols(reportColumns, dbRow);
      combined.addAll(simpleCols);

      List<String> repeatingCols = getProcessRepeatingCols(repeatingColumns, dbRow);
      combined.addAll(repeatingCols);

      List<String> checkboxCols = getProcessCheckboxCols(checkboxColumns, dbRow);
      combined.addAll(checkboxCols);

      if (simpleCols.size() + repeatingCols.size() + checkboxCols.size()> maxCol){
        maxCol = simpleCols.size() + repeatingCols.size() + checkboxCols.size();
      }

      cols.add( combined.toArray(new String[0]));
    }
    dbDataObj.setData(cols);
    dbDataObj.setHeader(reportColumns);
    dbDataObj.setMaxCol(maxCol);
    return dbDataObj;
  }

  private List<String> getProcessSimpleCols(List<String> reportColumns, DbRow row) {
    return reportColumns.stream()
        .map(col -> row.getSimpleCols().getOrDefault(col, ""))
        .collect(Collectors.toList());
  }

  private List<String> getProcessRepeatingCols(List<String> repeatingColumns, DbRow row) {

    Map<String, Map<String, String>> grouped = new HashMap<>();

    for (Map.Entry<String, String> entry : row.getRepeatingCols().entrySet()) {

      String[] parts = entry.getKey().split("\\|"); // IMPORTANT

      String key = parts[0];
      String uuid = parts[1];
      String value = entry.getValue();

      grouped
          .computeIfAbsent(uuid, k -> new HashMap<>())
          .put(key, value);
    }
    List<String> result = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> group : grouped.entrySet()) {

      String uuid = group.getKey();
      Map<String, String> values = group.getValue();

      for (String col : repeatingColumns) {
        result.add(col.concat("-").concat(uuid).concat("=").concat(values.getOrDefault(col, "")));
      }
    }

    return result;
  }
  private List<String> getProcessCheckboxCols(List<String> repeatingColumns, DbRow row) {

    Map<String, Map<String, String>> grouped = new HashMap<>();

    for (Map.Entry<String, String> entry : row.getCheckboxCols().entrySet()) {

      String[] parts = entry.getKey().split("\\|"); // IMPORTANT

      String key = parts[0];
      String uuid = parts[1];
      String value = entry.getValue();

      grouped
          .computeIfAbsent(uuid, k -> new HashMap<>())
          .put(key, value);
    }
    List<String> result = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> group : grouped.entrySet()) {

      String uuid = group.getKey();
      Map<String, String> values = group.getValue();

      for (String col : repeatingColumns) {
        result.add(col.concat("-").concat(uuid).concat("=").concat(values.getOrDefault(col, "")));
      }
    }

    return result;
  }

  @Setter @Getter
  public static class SimpleCols extends HashMap<String, String> {

  }
  @Setter @Getter
  public static class RepeatingCols extends HashMap<String, String> {

  }
  @Setter @Getter
  public static class CheckboxCols extends HashMap<String, String> {

  }

  @Setter @Getter
  @AllArgsConstructor
  public static class DbRow {
    String identifier;
    SimpleCols  simpleCols;
    RepeatingCols   repeatingCols;
    CheckboxCols   checkboxCols;
  }

  private DbDataObj getDataFromDB(UUID planIdentifier,
      DataExtractQuery target) {

    List<String> header = new ArrayList<>();

    List<String[]> data = jdbcTemplate.query(
        target.getQuery(),
        ps -> ps.setString(1, "%".concat(planIdentifier.toString()).concat("%")), (rs, rowNum) -> {
          int columnCount = rs.getMetaData().getColumnCount();

          ResultSetMetaData metaData = rs.getMetaData();
          String[] row = new String[columnCount];

          if (rowNum == 0) {  // We only need to get the header once (for the first row)
            for (int i = 1; i <= columnCount; i++) {
              header.add(metaData.getColumnName(i));  // Add column names to header list
            }
            log.debug("header {}", header);
          }

          for (int i = 1; i <= columnCount; i++) {
            // Convert each column to a string, even if it's not a string in the database
            row[i - 1] = rs.getString(i); // Get column value as string
          }

          return row;
        });

    return new DbDataObj(header,data, null);
  }

  public String getCode(UUID planIdentifier, String queryLabel){

    List<DataExtractQuery> byPlanIdentifier = dataExtractQueryRepository.findByPlanIdentifierAndQueryLabel(
        planIdentifier, queryLabel);

    if (byPlanIdentifier == null || byPlanIdentifier.size() == 0) {
      throw new NotFoundException("No Query found for plan " + planIdentifier);
    }

    return byPlanIdentifier.get(0).getQuery();
  }

  public List<DataExtractQueryResponse> getQueryLabels(UUID planIdentifier){

    List<DataExtractQuery> dataExtractQueries = dataExtractQueryRepository.findByPlanIdentifier(
        planIdentifier);

    if (dataExtractQueries == null || dataExtractQueries.size() == 0) {
      throw new NotFoundException("No Query found for plan " + planIdentifier);
    }

    return dataExtractQueries.stream().map(dataExtractQuery -> DataExtractQueryResponse.builder()
        .queryLabel(dataExtractQuery.getQueryLabel())
        .id(dataExtractQuery.getId())
        .planIdentifier(dataExtractQuery.getPlanIdentifier())
        .build()).collect(Collectors.toList());
  }
}
