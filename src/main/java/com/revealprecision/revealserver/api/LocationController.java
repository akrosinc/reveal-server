package com.revealprecision.revealserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.service.LocationService;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping(value = "/location",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public Location createLocation(@RequestBody JsonNode data){
        ObjectMapper objectMapper = new ObjectMapper();
        String geoLevelName = objectMapper.convertValue(data.get("geographicLevel"),String.class);
        GeographicLevel geographicLevel = geographicLevelRepository.findByName(geoLevelName);
        Location location = objectMapper.convertValue(data.get("location"),Location.class);
        location.setGeographicLevel(geographicLevel);
        return locationService.createLocation(location);
    }
}
