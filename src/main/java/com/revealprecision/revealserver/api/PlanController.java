package com.revealprecision.revealserver.api.controller;

import java.util.concurrent.atomic.AtomicLong;

import com.revealprecision.revealserver.service.PlanService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("api/v1/")
public class PlanController {

	// @Operation(summary = "Search for plan definitions",
	// 	description = "Search for plan definitions using path variables as filters",
	// 	tags = { "Plan" }
	// )
	@GetMapping(value = "/plan",
		produces = "application/json"
	)
	public List<PlanResponse> searchPlans (
		// @Parameter(description = "Search by plan title")
		// @Pattern(regex = "[A-Za-z0-9\\-]*")
		// @Size(min = 3, max = 20, message = "title parameter length must be between 3 and 20 characters")
		@RequestParam(value = "title", required = false) String planTitle,
		// @Parameter(description = "Search by plan status")
		// @RequestParam(value = "status", required = false, defaultValue = "Active") Enum PlanStatusEnum,
		// @Parameter(description = "Search by plan interventionType")
		// @RequestParam(value = "interventionType", required = false) Enum PlanInterventionTypeEnum,
		// @Parameter(description = "Search by plan jurisdiction")
		// @RequestParam(value = "jurisdiction", required = false) String planJurisdiction,
		// @Parameter(description = "filter by pageNumber")
		@RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber,
		// @Parameter(description = "filter by pageSize")
		@RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {

			return new PlanService.searchPlans(planTitle, PageRequest.of(pageNumber, pageSize));
	}
}