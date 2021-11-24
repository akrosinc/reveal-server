package com.revealprecision.revealserver.persistence.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class Location {
    @Id
    @GeneratedValue
    private UUID identifier;
    private String name;
}
