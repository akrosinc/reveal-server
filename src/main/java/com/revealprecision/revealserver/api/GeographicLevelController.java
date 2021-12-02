package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.service.GeographicLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    @Operation(summary = "Create a geographicLevel",
            description = "Create a geographicLevel",
            tags = { "Location" }
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/geographicLevel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object create(@Valid @RequestBody GeographicLevel geographicLevel){
        if(geographicLevelService.isGeographicLevelExist(geographicLevel)){
            return new ResponseEntity<>("Already  Exists",HttpStatus.CONFLICT);
        }
        return geographicLevelService.createGeographicLevel(geographicLevel);
    }

    @Operation(summary = "List geographicLevels",
            description = "List geographicLevels",
            tags = { "Location" }
    )
    @GetMapping(value = "/geographicLevel", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<GeographicLevel> getGeographicLevels(
            @Parameter(description = "Page number to return") @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
            @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50", required = false) Integer pageSize){
        return geographicLevelService.getGeographicLevels(pageNumber,pageSize);
    }

    @Operation(summary = "Fetch a geographicLevel",
            description = "Fetch a geographicLevel",
            tags = { "Location" }
    )
    @GetMapping(value = "/geographicLevel/{identifier}",produces = MediaType.APPLICATION_JSON_VALUE)
    public Object findGeographicLevelByIdentifier(
            @Parameter(description = "Identifier of the geographicLevel") @PathVariable UUID identifier){
                Optional<GeographicLevel> levelOptional = geographicLevelService.findGeographicLevelByIdentifier(identifier);
                if(levelOptional.isPresent()){
                    return levelOptional.get();
                }
                return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Update a geographicLevel",
            description = "Update a geographicLevel",
            tags = { "Location" }
    )
    @PutMapping(value = "/geographicLevel/{identifier}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeographicLevel updateGeographicLevel(
            @RequestBody GeographicLevel geographicLevel,
            @Parameter(description = "Identifier of the geographicLevel") @PathVariable UUID identifier){
        return geographicLevelService.update(identifier,geographicLevel);
    }


}
