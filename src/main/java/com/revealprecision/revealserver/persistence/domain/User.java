package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.UserUpdateRequest;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.Email;
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
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class User extends AbstractAuditableEntity {

    @ManyToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinTable(
            name = "user_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "organization_id"))
    Set<Organization> organizations;
    @Id
    @GeneratedValue
    private UUID identifier;
    @Column
    private UUID sid;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String userName;
    @Column(nullable = false)
    @Email
    private String email;
    @ElementCollection
    @CollectionTable(name = "user_security_groups", joinColumns = @JoinColumn(name = "identifier"))
    @Column(name = "security_group")
    private Set<String> securityGroups;

    private String apiResponse;

    public User updateUser(UserUpdateRequest request) {
        this.firstName = request.getFirstName();
        this.lastName = request.getLastName();
        this.email = request.getEmail();
        return this;
    }
}
