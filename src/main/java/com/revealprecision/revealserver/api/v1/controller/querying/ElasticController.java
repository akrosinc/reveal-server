package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/reveal-elastic")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "reveal.elastic.enabled",
        havingValue = "true"
)
public class ElasticController {

  private final LocationElasticRepository locationElasticRepository;

  @GetMapping("/{identifier}")
  private LocationElastic getItemById(@PathVariable("identifier")UUID identifier){
    return locationElasticRepository.findById(identifier.toString()).orElseThrow(() -> new NotFoundException("location not found "));
  }

  @PostMapping("/{identifier}")
  private LocationElastic saveItemById(@PathVariable("identifier")UUID identifier, @RequestBody LocationElastic locationElastic){

    Optional<LocationElastic> optional = locationElasticRepository.findById(identifier.toString());
    optional.ifPresent(locationElastic1 ->  locationElasticRepository.save(locationElastic));
    if (optional.isPresent()){
      return locationElasticRepository.save(locationElastic);
    } else {
      throw new NotFoundException("location not found ");
    }
  }
}
