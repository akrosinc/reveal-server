package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.UserResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.UserRequest;
import com.revealprecision.revealserver.api.v1.dto.response.UserResponse;
import com.revealprecision.revealserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {


    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserRequest userRequest) {
        userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(value = "search", defaultValue = "") String search, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(UserResponseFactory.fromEntityPage(userService.searchUsers(search, pageable),
                        pageable));
    }

    @GetMapping(value = "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> getUser(@PathVariable("identifier") UUID identifier) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(UserResponseFactory.fromEntity(userService.getByIdentifier(identifier)));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteUser(@PathVariable("identifier") UUID identifier) {
        userService.deleteUser(identifier);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Void> update(@PathVariable("identifier") UUID identifier,
                                       @Valid @RequestBody UserRequest userRequest) {
        userService.updateUser(identifier, userRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
