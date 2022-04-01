package com.revealprecision.revealserver.batch.writer;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class LocationWriter implements ItemWriter<Location> {

  private final LocationElasticRepository locationElasticRepository;
  private final LocationRepository locationRepository;

  @Override
  public void write(List<? extends Location> items) throws Exception {
    items = locationRepository.saveAll(items);
    List<LocationElastic> locations = new ArrayList<>();
    items.forEach(location -> {
      LocationElastic loc = new LocationElastic();
      loc.setId(location.getIdentifier().toString());
      loc.setLevel(location.getGeographicLevel().getName());
      loc.setGeometry(location.getGeometry());
      locations.add(loc);
    });
    locationElasticRepository.saveAll(locations);
  }
}
