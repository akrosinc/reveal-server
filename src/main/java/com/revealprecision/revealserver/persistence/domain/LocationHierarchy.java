package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@TypeDef(
        name = "list-array",
        typeClass = ListArrayType.class
)
public class LocationHierarchy extends AbstractAuditableEntity {
    @Id
    @GeneratedValue
    private UUID identifier;
    @NotEmpty(message = "node order list  is required and must not be empty")
    @Type(type = "list-array")
    private List<String> node_order;
}
