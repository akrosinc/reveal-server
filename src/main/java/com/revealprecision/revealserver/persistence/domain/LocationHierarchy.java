package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private List<String> nodeOrder;
}
