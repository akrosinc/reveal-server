package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GeographicLevelService {
    private GeographicLevelRepository geographicLevelRepository;

    @Autowired
    public GeographicLevelService(GeographicLevelRepository geographicLevelRepository) {
        this.geographicLevelRepository = geographicLevelRepository;
    }

    public GeographicLevel createGeographicLevel(GeographicLevel geographicLevel) {
        return geographicLevelRepository.save(geographicLevel);
    }

    public Page<GeographicLevel> getGeographicLevels(Integer pageNumber, Integer pageSize){
        return geographicLevelRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }

    public Optional<GeographicLevel> findGeographicLevelByIdentifier(UUID identifier){
        return geographicLevelRepository.findById(identifier);
    }

    public GeographicLevel update(UUID identifier,GeographicLevel geographicLevel){
        Optional<GeographicLevel> updateGeographicLevelOptional = geographicLevelRepository.findById(identifier);
        if(updateGeographicLevelOptional.isPresent()){
            GeographicLevel updateGeographicLevel = updateGeographicLevelOptional.get();
            updateGeographicLevel.setTitle(geographicLevel.getTitle());
            return geographicLevelRepository.save(updateGeographicLevel);
        }
        return  null;
    }

    public GeographicLevel findByName(String name){
        return geographicLevelRepository.findByName(name);
    }

    public Boolean isGeographicLevelExist(GeographicLevel geographicLevel){
        return geographicLevelRepository.existsByNameAndTitle(geographicLevel.getName(),geographicLevel.getTitle());
    }
}
