package com.revealprecision.revealserver.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reveal.raster.ingestion")
@Getter
@Setter
public class RasterIngestionProperties {

  private String basePath;
  private String tilesSubdirName = "tiles";
}
