package com.revealprecision.revealserver.persistence.domain;

import lombok.*;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserBulkException extends AbstractAuditableEntity {

    @Id
    @GeneratedValue
    private UUID identifier;

    private String username;

    private String message;

    @ManyToOne
    @JoinColumn(name = "user_bulk_identifier")
    private UserBulk userBulk;
}
