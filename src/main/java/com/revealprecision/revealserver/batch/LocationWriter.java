package com.revealprecision.revealserver.batch;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationWriter implements ItemWriter<Location> {

  @Autowired
  private LocationElastic locationElastic;

  @Autowired
  private LocationRepository locationRepository;

  @Override
  public void write(List<? extends Location> items) throws Exception {
    items = locationRepository.saveAll(items);
    List<com.revealprecision.revealserver.persistence.es.Location> locations = new ArrayList<>();
    items.forEach(location -> {
      com.revealprecision.revealserver.persistence.es.Location loc = new com.revealprecision.revealserver.persistence.es.Location();
      loc.setId(location.getIdentifier().toString());
      loc.setLevel(location.getGeographicLevel().getName());
      loc.setGeometry(location.getGeometry());
      locations.add(loc);
    });
    locationElastic.saveAll(locations);
  }
}
