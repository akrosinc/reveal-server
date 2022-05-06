package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID>,
    JpaSpecificationExecutor<Person> {

  Optional<Person> findByIdentifier(UUID identifier);

  Page<Person> findPersonByBirthDate(Date date, Pageable pageable);

  Long countPersonByBirthDate(Date date);

  List<Person> findPersonByLocationsIn(List<Location> locations);

}