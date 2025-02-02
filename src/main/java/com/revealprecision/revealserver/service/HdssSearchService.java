package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualObj;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSearchRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HdssSearchService {

  private final JdbcTemplate jdbcTemplate;

  public List<HdssCompoundHouseholdIndividualObj> searchHdssCompounds(
      HdssSearchRequest searchRequest) {
    StringBuilder sql = new StringBuilder("SELECT "
        + "id,"
        + "compound_id,"
        + "household_id,"
        + "individual_id,"
        + "name,"
        + "fields->>'gender' as gender,"
        + "fields->>'dob' as dob,"
        + "server_version as serverVersion"

        + " FROM hdss.hdss_compounds WHERE 1=1");
    List<Object> params = new ArrayList<>();

// Add conditions for searchString (compoundId, householdId, individualId) with case-insensitive comparison
    if (searchRequest.getSearchString() != null && !searchRequest.getSearchString().isEmpty()) {
      sql.append(
          " AND (LOWER(compound_id) LIKE LOWER(?) OR LOWER(household_id) LIKE LOWER(?) OR LOWER(individual_id) LIKE LOWER(?))");
      String searchString = "%" + searchRequest.getSearchString().toLowerCase()
          + "%";  // Make sure search string is lowercased
      params.add(searchString);
      params.add(searchString);
      params.add(searchString);
    }

// Add condition for name with case-insensitive comparison
    if (searchRequest.getName() != null) {
      sql.append(" AND LOWER(name) LIKE LOWER(?)");
      params.add("%" + searchRequest.getName().toLowerCase() + "%");
    }

// Add condition for gender in the fields JSONB with case-insensitive comparison
    if (searchRequest.getGender() != null) {
      sql.append(" AND LOWER(fields->>'gender') = LOWER(?)");
      params.add(searchRequest.getGender().toLowerCase());  // Make sure gender is lowercased
    }

// Add condition for dob in the fields JSONB
    if (searchRequest.getDob() != null) {
      String dobStr = searchRequest.getDob();
      DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");

      LocalDate dob = null;

      // Try parsing with the first format (yyyy-MM-dd)
      try {
        dob = LocalDate.parse(dobStr, formatter1);
      } catch (DateTimeParseException e) {
        // If it fails, try parsing with the second format (dd-MM-yyyy)
        try {
          dob = LocalDate.parse(dobStr, formatter2);
        } catch (DateTimeParseException ex) {
          // Handle error if both formats fail
          throw new IllegalArgumentException("Invalid date format. Expected 'yyyy-MM-dd' or 'dd-MM-yyyy'.");
        }
      }

      // Format the LocalDate to yyyy-MM-dd string
      DateTimeFormatter yyyymmddformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      String formattedDob = dob.format(yyyymmddformatter);

      sql.append(" AND fields->>'dob' = ?");
      params.add(formattedDob);
    }

// Convert params List to an Object array
    Object[] paramsArray = params.toArray(new Object[0]);

// Use JdbcTemplate to execute the query
    List<HdssCompoundHouseholdIndividualObj> result = jdbcTemplate.query(
        sql.toString(),

        (rs, rowNum) -> HdssCompoundHouseholdIndividualObj
            .builder()
            .id(rs.getString("id"))
            .compoundId(rs.getString("compound_id"))
            .householdId(rs.getString("household_id"))
            .individualId(rs.getString("individual_id"))
            .name(rs.getString("name"))
            .dob(rs.getString("dob"))
            .gender(rs.getString("gender"))
            .serverVersion(rs.getInt("serverVersion"))
            .build(),
        paramsArray
    );
    return result;
  }
}
