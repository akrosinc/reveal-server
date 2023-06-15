package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.Person;
import java.io.IOException;

public interface MetadataEsService {
   void updatePersonDetailsOnElasticSearch(Person person) throws IOException;
}
