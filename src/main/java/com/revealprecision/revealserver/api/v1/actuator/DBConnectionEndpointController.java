package com.revealprecision.revealserver.api.v1.actuator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Endpoint(id="dbconnection")
@Component
@RequiredArgsConstructor
public class DBConnectionEndpointController {


  private final JdbcTemplate jdbcTemplate;
  @ReadOperation
  @Bean
  private String getDBConn(){

    List<String> query = jdbcTemplate.query("SELECT '1' as out", (rs, num) -> rs.getString("out"));

    return query.get(0);
  }

}
