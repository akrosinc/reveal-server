package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.repository.GeographicLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
