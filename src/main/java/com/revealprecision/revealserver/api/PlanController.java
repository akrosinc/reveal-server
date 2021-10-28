package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/")
public class PlanController {
	private PlanService planService;

	@Autowired
	public PlanController(PlanService planService) {
		this.planService = planService;
	}
	@Operation(summary = "Fetch All Plans",
			description = "Fetch All Plans",
			tags = { "Plan" }
	)
	@GetMapping(value = "/plan",
			produces = "application/json"
	)
	public List<Plan> getPlans(){
		return planService.getPlans();
	}
}