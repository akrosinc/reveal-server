package com.revealprecision.revealserver.api.v1.facade.dto.service;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.PlanService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeService {

  private  final PlanService planService;
  public List<Plan> geAssignedPlans(User user){
    //here we will return the plans assigned to the user:
    //TODO: final logic for finding the plans assigned to user once we know assignment (maybe obtain via organization assigned to user)00

    UUID planIdentifier = UUID.fromString("0f2c048a-762b-4067-9ab1-8031cbf87a0b");
    return Arrays.asList(planService.getPlanByIdentifier(planIdentifier));
  }

}
