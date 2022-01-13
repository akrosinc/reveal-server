package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {
     Optional<Person> findByIdentifier(UUID identifier);

//     List<Person> findPersonByNameFamilyLikeOrBAndNameGivenLikeOrNamePrefixLikeOrNameTextLikeOOrNamePrefixOR
}