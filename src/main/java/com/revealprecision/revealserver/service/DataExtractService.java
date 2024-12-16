package com.revealprecision.revealserver.service;

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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

  public InputStreamResource extract(UUID planIdentifier) throws IOException {
    StringWriter stringWriter = new StringWriter();

    List<String> header = new ArrayList<>();

    List<DataExtractQuery> firstByPlanIdentifier = dataExtractQueryRepository.findByPlanIdentifier(
        planIdentifier);

    if (firstByPlanIdentifier == null || firstByPlanIdentifier.size() == 0) {
      throw new NotFoundException("No Query found for plan " + planIdentifier);
    }
    DataExtractQuery target = firstByPlanIdentifier.get(0);

    if (!target.getQuery().isEmpty()) {

      List<String[]> data = jdbcTemplate.query(
          target.getQuery(),
          ps -> ps.setString(1, planIdentifier.toString()), (rs, rowNum) -> {
            int columnCount = rs.getMetaData().getColumnCount();
            ResultSetMetaData metaData = rs.getMetaData();
            String[] row = new String[columnCount];

            if (rowNum == 0) {  // We only need to get the header once (for the first row)
              for (int i = 1; i <= columnCount; i++) {
                header.add(metaData.getColumnName(i));  // Add column names to header list
              }
            }

            for (int i = 1; i <= columnCount; i++) {
              // Convert each column to a string, even if it's not a string in the database
              row[i - 1] = rs.getString(i); // Get column value as string
            }

            return row;
          });

      RowWriter rowWriter = new RowWriterImpl(stringWriter);

      rowWriter.writeHeader(header.toArray(new String[0]));

      for (String[] row : data) {
        List<String> strArr = new ArrayList<>(Arrays.asList(row));
        rowWriter.writeRow(strArr.toArray(new String[0]));
      }
      stringWriter.close();
      InputStream targetStream = new ByteArrayInputStream(stringWriter.toString().getBytes());
      return new InputStreamResource(targetStream);
    }
    throw new NotFoundException("No Query found for plan " + planIdentifier);
  }

  public String getCode(UUID planIdentifier){

    List<DataExtractQuery> byPlanIdentifier = dataExtractQueryRepository.findByPlanIdentifier(
        planIdentifier);

    if (byPlanIdentifier == null || byPlanIdentifier.size() == 0) {
      throw new NotFoundException("No Query found for plan " + planIdentifier);
    }

    return byPlanIdentifier.get(0).getQuery();
  }
}
