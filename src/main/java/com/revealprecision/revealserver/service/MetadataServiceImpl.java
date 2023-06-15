package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!Elastic")
public class MetadataServiceImpl implements MetadataEsService {

  public void updatePersonDetailsOnElasticSearch(Person person)  {
    log.info("Elastic profile disabled, person not updated on ES");
  }
}
