package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
public class GeographicLevel extends AbstractAuditableEntity {
    @Id
    @GeneratedValue
    private UUID identifier;

    @NotBlank(message = "title is required and must not be empty")
    private  String title;

    @NotBlank(message = "name is required and must not be empty")
    private String name;
}
