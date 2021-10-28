package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.util.Mapper;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlanService {

    public List<PlanResponse> searchPlans(String planTitle) {

        List<Plan> plansByTitle = planRespository.searchPlans(planTitle);
        List<PlanResponse> collect = plansByTitle.stream()
                .map(Mapper::getPlanResponse)
                .map()
                .collect(Collectors.toList());

        return collect;

    }
}