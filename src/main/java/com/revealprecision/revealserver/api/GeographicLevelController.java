package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.service.GeographicLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
@Validated
public class GeographicLevelController {
    private GeographicLevelService geographicLevelService;

    @Autowired
    public GeographicLevelController(GeographicLevelService geographicLevelService) {
        this.geographicLevelService = geographicLevelService;
    }

    @PostMapping(value = "/geographicLevel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeographicLevel create(@RequestBody GeographicLevel geographicLevel){
        return geographicLevelService.createGeographicLevel(geographicLevel);
    }

    @GetMapping(value = "/geographicLevel", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<GeographicLevel> getGeographicLevels(@RequestParam(defaultValue = "0") Integer pageNumber, @RequestParam(defaultValue = "50") Integer pageSize){
        return geographicLevelService.getGeographicLevels(pageNumber,pageSize);
    }

    @GetMapping(value = "/geographicLevel/{identifier}",produces = MediaType.APPLICATION_JSON_VALUE)
    public Object findGeographicLevelByIdentifier(@PathVariable UUID identifier){
        Optional<GeographicLevel> levelOptional = geographicLevelService.findGeographicLevelByIdentifier(identifier);
        if(levelOptional.isPresent()){
            return levelOptional.get();
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(value = "/geographicLevel/{identifier}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeographicLevel updateGeographicLevel(@RequestBody GeographicLevel geographicLevel,@PathVariable UUID identifier){
        return geographicLevelService.update(identifier,geographicLevel);
    }


}
