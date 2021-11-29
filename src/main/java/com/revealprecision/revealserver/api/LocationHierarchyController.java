package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchyWrapper;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/")
public class LocationHierarchyController {
    private LocationHierarchyService locationHierarchyService;

    @Autowired
    public LocationHierarchyController(LocationHierarchyService locationHierarchyService) {
        this.locationHierarchyService = locationHierarchyService;
    }

    @Operation(summary = "Create a locationHierarchy",
            description = "Create a locationHierarchy",
            tags = { "Location" }
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/locationHierarchy",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public LocationHierarchy create(@Validated @RequestBody LocationHierarchyWrapper locationHierarchy){
        List<String> hierarchyNodes =  locationHierarchy.getNode_order();
        LocationHierarchy locationHierarchySave = new LocationHierarchy();
        locationHierarchySave.setNode_order(hierarchyNodes);
        return locationHierarchyService.createLocationHierarchy(locationHierarchySave);
    }

    @GetMapping(value = "/locationHierarchy",produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<LocationHierarchy> getLocationHierachies(@RequestParam(defaultValue = "0") Integer pageNumber, @RequestParam(defaultValue = "50") Integer pageSize){
        return locationHierarchyService.getLocationHierarchies(pageNumber,pageSize);
    }
}
