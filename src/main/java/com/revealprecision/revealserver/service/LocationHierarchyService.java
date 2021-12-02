package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public Page<LocationHierarchy> getLocationHierarchies(Integer pageNumber, Integer pageSize){
        return locationHierarchyRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }

    public Boolean isLocationHierarchyExists(LocationHierarchy locationHierarchy){
        //TODO: implement
        return  false;
    }
}
