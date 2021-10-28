package com.revealprecision.revealserver.util;

public class Mapper {
    public static PlanResponse getPlanResponse(Plan p) {
        PlanResponse response = new PlanResponse();
        response.setId(p.getId());
        return response;
    }
}