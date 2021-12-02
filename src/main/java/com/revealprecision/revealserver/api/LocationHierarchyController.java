package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchyWrapper;
import com.revealprecision.revealserver.service.GeographicLevelService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/")
public class LocationHierarchyController {
    private final LocationHierarchyService locationHierarchyService;
    private final GeographicLevelService geographicLevelService;
    private final JobScheduler jobScheduler;
    private final LocationRelationshipService locationRelationshipService;

    @Autowired
    public LocationHierarchyController(LocationHierarchyService locationHierarchyService,
                                       GeographicLevelService geographicLevelService,
                                       JobScheduler jobScheduler,
                                       LocationRelationshipService locationRelationshipService) {
        this.locationHierarchyService = locationHierarchyService;
        this.geographicLevelService = geographicLevelService;
        this.jobScheduler = jobScheduler;
        this.locationRelationshipService = locationRelationshipService;
    }

    @Operation(summary = "Create a locationHierarchy",
            description = "Create a locationHierarchy",
            tags = { "Location" }
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/locationHierarchy",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public Object create(@Validated @RequestBody LocationHierarchyWrapper locationHierarchy){
        List<String> hierarchyNodes = locationHierarchy.getNode_order();

        if(hierarchyNodes.stream().filter(node -> Collections.frequency(hierarchyNodes,node) > 1).findAny().isPresent()){
            return new ResponseEntity<>("duplicate nodes on hierarchy",HttpStatus.UNPROCESSABLE_ENTITY);
        }
       for(String node: hierarchyNodes) {
           GeographicLevel found = geographicLevelService.findByName(node);
           if(found == null){
               return new ResponseEntity<>(String.format("%s does not exist",node),HttpStatus.UNPROCESSABLE_ENTITY);
           }
       }
        LocationHierarchy locationHierarchySave = new LocationHierarchy();
        locationHierarchySave.setNodeOrder(hierarchyNodes);

        if(locationHierarchyService.isLocationHierarchyExists(locationHierarchySave)){
            return new ResponseEntity<>("Already Exists",HttpStatus.CONFLICT);
        }
        LocationHierarchy saved =  locationHierarchyService.createLocationHierarchy(locationHierarchySave);
        if(saved != null){
            jobScheduler.enqueue(() -> locationRelationshipService.createLocationRelationships(saved));
        }
        return  saved;
    }

    @GetMapping(value = "/locationHierarchy",produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<LocationHierarchy> getLocationHierarchies(@RequestParam(defaultValue = "0") Integer pageNumber, @RequestParam(defaultValue = "50") Integer pageSize){
        return locationHierarchyService.getLocationHierarchies(pageNumber,pageSize);
    }
}
