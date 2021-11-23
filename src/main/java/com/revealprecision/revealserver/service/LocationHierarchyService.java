package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationHierarchyService {
    private LocationHierarchyRepository locationHierarchyRepository;

    @Autowired
    public LocationHierarchyService(LocationHierarchyRepository locationHierarchyRepository) {
        this.locationHierarchyRepository = locationHierarchyRepository;
    }

    public LocationHierarchy createLocationHierarchy(LocationHierarchy locationHierarchy){
        return locationHierarchyRepository.save(locationHierarchy);
    }
}
