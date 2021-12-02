package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationRelationshipService {
    private LocationRelationshipRepository locationRelationshipRepository;
    private GeographicLevelRepository geographicLevelRepository;
    private LocationRepository locationRepository;

    @Autowired
    public LocationRelationshipService(LocationRelationshipRepository locationRelationshipRepository, GeographicLevelRepository geographicLevelRepository,LocationRepository locationRepository) {
        this.locationRelationshipRepository = locationRelationshipRepository;
        this.geographicLevelRepository = geographicLevelRepository;
        this.locationRepository = locationRepository;
    }

    public void createLocationRelationships(LocationHierarchy locationHierarchy){
        List<String> nodes  = locationHierarchy.getNodeOrder();
        List<GeographicLevel> geographicLevels = nodes.stream().map(node ->  geographicLevelRepository.findByName(node)).collect(Collectors.toList());
        Map<GeographicLevel,List<Location>> geographicLevelToLocationsMap  = new LinkedHashMap<>();

        geographicLevels.forEach(geographicLevel -> {
            List<Location> locations = locationRepository.findByGeographicLevel(geographicLevel.getIdentifier());
            geographicLevelToLocationsMap.put(geographicLevel,locations);
        });


        geographicLevelToLocationsMap.entrySet().stream().forEach(item ->{
           List<Location> parentLocations =  item.getValue();
           List<Location> potentialChildren  = geographicLevelToLocationsMap.get(geographicLevels.get(geographicLevels.indexOf(item.getKey()) + 1));
           if(potentialChildren != null){
               parentLocations.stream().forEach(location -> potentialChildren.stream().forEach(potentialChild ->{
                   if(locationRepository.hasParentChildRelationship(location.getGeometry().toString(),potentialChild.getGeometry().toString())){
                       //then a relationship exist so we need to update LocationHierarchy object
                       LocationRelationship locationRelationship = new LocationRelationship();
                       locationRelationship.setParent_identifier(location.getIdentifier());
                       locationRelationship.setLocation_hierarchy_identifier(locationHierarchy.getIdentifier());
                       locationRelationship.setLocation_identifier(potentialChild.getIdentifier());
                       locationRelationshipRepository.save(locationRelationship);
                   }
               }));
           }
        });

    }

}
