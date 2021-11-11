package com.revealprecision.revealserver.persistence.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PlanPayload implements Serializable {
    private String identifier;
    private String name;
    private String title;
    private Enum PlanStatusEnum;
    private Date date;
    private Date effectivePeriodStart;
    private Date effectivePeriodEnd;
    private Enum interventionType;
}


/*

    "identifier": guid,
    "name": string,
    "title": string,
    "status": enum, // draft, active, retired, unknown
    "date": date, // last_modified_date
    "effectivePeriod": {
        "start": date,
        "end": date
    },
    "useContext": [
        {
            "code": "interventionType",
            "valueCodableConcept": enum // IRS, IRS-Lite, MDA, MDA-Lite
        }
    ],
    "jurisdiction": [
        {
            "code": [guid,guid,guid]
        }
    ],
    "goal": [
        {
            "identifier": string,
            "description": string,
            "priority": enum, // high-priority, medium-priority, low-priority
            "target": [
                {
                    "measure": string,
                    "detail": {
                        "detailQuantity": {
                            "value": int,
                            "comparator": "gt;=",
                            "unit": enum // percent
                        }
                    },
                    "due": date
                }
            ]
        }
    ],
    "action": [
        {
            "identifier": guid,
            "title": string,
            "description": string,
            "code": string,
            "timingPeriod": {
                "start": date,
                "end": date
            },
            "reason": "Routine",
            "goalId": fk_to_goalIdentifier,
            "subjectCodableConcept": {
                "text": enum, // location, person
            },
            "trigger": [
                {
                    "type": "named-event",
                    "name": enum // plan-activation, plan-jurisdiction-modification, event-creation
                }
            ],
            "condition": [
                {
                    "kind": "applicability",
                    "expression": {
                        "description": "Structure type does not exist",
                        "expression": "$this.is(FHIR.Location)"
                    }
                }
            ],
            "definitionUri": string, // path to json
            "type": enum // create, update, remove
        }
    ]


 */