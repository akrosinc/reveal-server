package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class LocationService {
    private LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location createLocation(Location location){
        return locationRepository.save(location);
    }

    public Optional<Location> findByIdentifier(UUID identifier){
        return locationRepository.findById(identifier);
    }

    public Page<Location> getLocations(Integer pageNumber, Integer pageSize){
        return locationRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }
}
