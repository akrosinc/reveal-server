package com.revealprecision.revealserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
public class LocationController {
    private LocationService locationService;
    private GeographicLevelRepository geographicLevelRepository;

    @Autowired
    public LocationController(LocationService locationService, GeographicLevelRepository geographicLevelRepository) {
        this.locationService = locationService;
        this.geographicLevelRepository = geographicLevelRepository;
    }

    @Operation(summary = "Create a location",
            description = "Create a location",
            tags = { "Location" }
    )
    @PostMapping(value = "/location",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public Location createLocation(@RequestBody JsonNode data){
        ObjectMapper objectMapper = new ObjectMapper();
        String geoLevelName = objectMapper.convertValue(data.get("geographicLevel"),String.class);
        GeographicLevel geographicLevel = geographicLevelRepository.findByName(geoLevelName);
        Location location = objectMapper.convertValue(data.get("location"),Location.class);
        location.setGeographicLevel(geographicLevel);
        return locationService.createLocation(location);
    }

    @GetMapping(value = "/location/{identifier}",produces = MediaType.APPLICATION_JSON_VALUE)
    public Object findLocationById(@PathVariable UUID identifier){
        Optional<Location> locationOptional = locationService.findByIdentifier(identifier);
        if(locationOptional.isPresent())
            return locationOptional.get();
        return new ResponseEntity<Location>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(value = "/location",produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Location> getLocations(@RequestParam(defaultValue = "0") Integer pageNumber, @RequestParam(defaultValue = "50") Integer pageSize){
        return locationService.getLocations(pageNumber,pageSize);
    }
}
