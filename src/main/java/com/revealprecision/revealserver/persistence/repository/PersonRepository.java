package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Person;
import java.util.Date;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID>,
    JpaSpecificationExecutor<Person> {
     Optional<Person> findByIdentifier(UUID identifier);

//     List<Person> findPersonByNameFamilyContainingIgnoreCaseOrNameGivenContainingIgnoreCaseOrNameTextContainingIgnoreCaseOrNamePrefixContainingIgnoreCaseOrNameSuffixContainingIgnoreCaseOrNameUseContainingIgnoreCaseOrGenderContainingIgnoreCase(
//         String family,
//         String given,
//         String text,
//         String prefix,
//         String Suffix,
//         String use,
//         String gender);



     Page<Person> findPersonByBirthDate(
         Date date, Pageable pageable
     );

     Long countPersonByBirthDate(
         Date date
     );
     //    searchFirstName -> name_given, name_text
//    searchLastName -> name_family, name_suffix
//    searchLocation -> Person.Group.location.name
//    searchGroup -> Person.Group.name

//     List<Person> findPersonByNameGivenOrNameTextAndNameFamilyOrNameSuffix();
     List<Person> findPersonByGroups_NameAndGroups_Location_NameAndNameGivenOrNameTextAndNameFamilyOrNameSuffix(String groupName, String locationName,
         String givenName, String textName, String familyName, String suffix);




}