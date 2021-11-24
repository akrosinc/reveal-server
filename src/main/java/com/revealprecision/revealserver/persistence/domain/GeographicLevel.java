package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
public class GeographicLevel extends AbstractAuditableEntity {
    @Id
    @GeneratedValue
    private UUID identifier;
    @NotBlank(message = "title is required and must not be empty")
    private  String title;
    @NotBlank(message = "name is required and must not be empty")
    private String name;
}
