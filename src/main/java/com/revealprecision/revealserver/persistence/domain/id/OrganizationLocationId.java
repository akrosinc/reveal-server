package com.revealprecision.revealserver.persistence.domain.id;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrganizationLocationId implements Serializable {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "location_id")
    private UUID locationId;
}