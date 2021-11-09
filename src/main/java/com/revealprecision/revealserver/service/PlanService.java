package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PlanService {
    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);
    private PlanRepository planRepository;
    private final ProducerService producerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PlanService(PlanRepository planRepository, ProducerService producerService, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.producerService = producerService;
        this.objectMapper = objectMapper;
    }
    public List<Plan> getPlans(){
        return planRepository.findAll();
    }

    public Plan createPlan(Plan plan) {
        Plan save = planRepository.save(plan);
        logger.info(String.format("Plan saved to database as %s", plan));

        try {
            producerService.sendMessage(objectMapper.writeValueAsString(plan));
        } catch (JsonProcessingException e) {
            logger.debug(String.format("Plan not mapped %s", e.getMessage()));
            e.printStackTrace();
        }
        return save;
    }

    public Plan getPlanByIdentifer(String planIdentifier) {
        return PlanRepository.findAllByPlanIdentifier(planIdentifier);
    }
}