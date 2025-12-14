package com.revealprecision.revealserver.amdr.persistence.repository;

import com.revealprecision.revealserver.amdr.persistence.domain.AmdrMappings;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AmdrMappingsRepository extends JpaRepository<AmdrMappings, Integer> {

  AmdrMappings findFirstByAmdrKey(String amdrKey);

  @Query("Select a.amdrKey from AmdrMappings a")
  List<String> getAmdrKeys();

  @Query("Select a.amdrSubKeys from AmdrMappings a WHERE a.amdrKey = :amdrKey")
  List<List<String>> getAmdrSubKeysByAmdrKey(String amdrKey);

}
