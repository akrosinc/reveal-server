package com.revealprecision.revealserver.persistence.domain.model;

import java.util.Date;

/*
The person payload is defined to align to the FHIR object documented at https://www.hl7.org/fhir/person.html.

Roles and Relationships are not FHIR concepts but work to create the relationship tree between objects.
*/

public class PersonPayload {
    private String identifier;
    private Boolean active;
    //NAME OBJECT
    private Enum gender; // GenderEnum
    private Date birthDate;
    //ROLES OBJECT
    //RELATIONSHIPS
}


/*
{
    "identifier": guid,
    "active": boolean,
    "name": {
        "use": enum, // usual, official, temp, nickname, anonymous, old, maiden
        "text" : string, // text representation of the full name
        "family" : string, // family name (often called 'Surname')
        "given" : [string], // given names, includes middle names
        "prefix" : [string], // parts that come before the name
        "suffix" : [string], // parts that come after the name
    }
    "roles": [
        enum
    ],
    "relationships": [
        "family": [
            {
                "personIdentifier": guid,
                "relationType": enum,
                "relationStatus": enum
            }
        ],
        "locations": [
            {
                "locationIdentifier": guid,
                "relationType": enum,
                "relationStatus": enum
            }
        ]
    ]
}
 */