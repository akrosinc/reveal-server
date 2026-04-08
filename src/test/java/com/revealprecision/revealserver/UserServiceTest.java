package com.revealprecision.revealserver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.revealprecision.revealserver.api.v1.dto.request.RegisterUserRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.service.KeycloakService;
import com.revealprecision.revealserver.service.OrganizationService;
import com.revealprecision.revealserver.service.UserService;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private UserService userService;

    @Test
    public void testGenerateUsername_noConflict() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("John");
        request.setLastName("Doe");

        // Simulate that "john.doe" is not taken.
        when(userRepository.getByUsername("john.doe")).thenReturn(Optional.empty());

        // Act
        String username = userService.generateUsername(request);

        // Assert
        assertEquals("john.doe", username);
    }

    @Test
    public void testGenerateUsername_multipleConflicts() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Alice");
        request.setLastName("Smith");

        when(userRepository.getByUsername("alice.smith")).thenReturn(Optional.of(new User()));
        when(userRepository.getByUsername("alice.smith1")).thenReturn(Optional.of(new User()));
        when(userRepository.getByUsername("alice.smith2")).thenReturn(Optional.empty());

        // Act
        String username = userService.generateUsername(request);

        // Assert
        assertEquals("alice.smith2", username);
    }

    @Test
    public void testCreateUserForInvitation_emailConflict() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setOrganizations(Set.of(UUID.fromString("org1")));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User()));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () -> {
            userService.createUserForInvitation(request);
        });
        assertEquals("There already is a user with email: test@example.com", ex.getMessage());
    }

    @Test
    public void testCreateUserForInvitation_success() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        UUID organizationId = UUID.randomUUID();
        request.setEmail("john.doe@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setOrganizations(Set.of(organizationId));

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(userRepository.getByUsername("john.doe")).thenReturn(Optional.empty());

        Organization org = new Organization();
        org.setIdentifier(organizationId);
        org.setName("Organization One");
        when(organizationService.findByIdentifiers(any())).thenReturn(Set.of(org));

        String keycloakId = UUID.randomUUID().toString();
        when(keycloakService.createUserForInvitation(any())).thenReturn(keycloakId);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User createdUser = userService.createUserForInvitation(request);

        // Assert
        assertNotNull(createdUser);
        assertEquals("john.doe", createdUser.getUsername());
        assertEquals(UUID.fromString(keycloakId), createdUser.getSid());
        assertEquals(EntityStatus.ACTIVE, createdUser.getEntityStatus());
    }
}
