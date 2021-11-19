package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlanService {
    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    private final PlanRepository planRepository;
    private final ProducerService producerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlanService(PlanRepository planRepository, ProducerService producerService, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.producerService = producerService;
        this.objectMapper = objectMapper;
    }
    public Page<Plan> getPlans(Integer pageNumber, Integer pageSize){
        return planRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }

    public Plan createPlan(Plan plan) {
        Plan save = planRepository.save(plan);
        logger.info("Plan saved to database as {}", plan);

//        try {
//            producerService.sendMessage(objectMapper.writeValueAsString(plan));
//        } catch (JsonProcessingException e) {
//            logger.debug("Plan not mapped {}", e.getMessage());
//        }
        return save;
    }

    public Plan getPlanByIdentifier(UUID planIdentifier) {
        return planRepository.findByIdentifier(planIdentifier);
    }
}