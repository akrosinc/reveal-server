package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.service.GeographicLevelService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/")
@Validated
public class GeographicLevelController {
    private GeographicLevelService geographicLevelService;

    @Autowired
    public GeographicLevelController(GeographicLevelService geographicLevelService) {
        this.geographicLevelService = geographicLevelService;
    }

    @Operation(summary = "Create a geographicLevel",
            description = "Create a geographicLevel",
            tags = { "Location" }
    )
    @PostMapping(value = "/geographicLevel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeographicLevel create(@RequestBody GeographicLevel geographicLevel){
        return geographicLevelService.createGeographicLevel(geographicLevel);
    }

}
