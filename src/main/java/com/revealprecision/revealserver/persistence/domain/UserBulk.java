package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revealprecision.revealserver.enums.BulkStatusEnum;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@FieldNameConstants
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Where(clause = "entity_status='ACTIVE'")
public class UserBulk extends AbstractAuditableEntity {

    @Id
    @GeneratedValue
    private UUID identifier;

    @Column(nullable = false)
    private String filename;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]", timezone = "${spring.jackson.time-zone}")
    @Column(nullable = false)
    private LocalDateTime uploadDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BulkStatusEnum status;

    @OneToMany(mappedBy = "userBulk")
    private Set<User> users;

    @OneToMany(mappedBy = "userBulk")
    private Set<UserBulkException> userBulkExceptions;
}
