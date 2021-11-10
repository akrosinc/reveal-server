package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.util.List;

@RestController
@RequestMapping("api/v1/")
public class PlanController {
	private PlanService planService;

	@Autowired
	public PlanController(PlanService planService) {
		this.planService = planService;
	}
	@Operation(summary = "Fetch all Plans",
			description = "Fetch all Plans",
			tags = { "Plan" }
	)
	@ResponseStatus(HttpStatus.OK)
//	@RolesAllowed("view_plan")
	@GetMapping(value = "/plan",
			produces = "application/json"
	)
	public List<Plan> getPlans(){
		return planService.getPlans(); /// Add pagination
	}

//	@Operation(summary = "Fetch a Plan by identfier",
//			description = "Fetch a Plan by identfier",
//			tags = { "Plan" }
//	)
//	@ResponseStatus(HttpStatus.OK)
//	@GetMapping(value = "/plan/{identifier}",
//			produces = "application/json"
//	)
//	public Plan getPlanByIdentifier(@Parameter(description = "Plan identifier") @PathVariable("identifier") String planIdentifier){
//		return planService.getPlanByIdentifer(planIdentifier);
//	}

	@Operation(summary = "Create a plan",
			description = "Create a Plan",
			tags = { "Plan" }
	)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value = "/plan", consumes = "application/json", produces = "application/json")
	public Plan createPlan(@Validated @RequestBody Plan plan) {
		return planService.createPlan(plan);
	}
}