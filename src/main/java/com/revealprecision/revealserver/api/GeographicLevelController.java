package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.service.GeographicLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<GeographicLevel> getGeographicLevels(){
        return geographicLevelService.getGeographicLevels();
    }


}
