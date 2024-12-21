package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.facade.models.HdssCompoundHouseholdIndividualObj;
import com.revealprecision.revealserver.api.v1.facade.request.HdssSearchRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HdssSearchService {

  private final JdbcTemplate jdbcTemplate;

  public List<HdssCompoundHouseholdIndividualObj> searchHdssCompounds(HdssSearchRequest searchRequest) {
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
      sql.append(" AND fields->>'dob' = ?");
      params.add(searchRequest.getDob());
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
