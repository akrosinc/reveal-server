package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public class LocationController {
    private LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "Create a location",
            description = "Create a location",
            tags = { "Location" }
    )
    @PostMapping(value = "/location",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public Location createLocation(@RequestBody Location location){
        return locationService.createLocation(location);
    }
}
