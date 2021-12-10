package com.revealprecision.revealserver.config;

import com.cosium.spring.data.jpa.entity.graph.repository.support.EntityGraphJpaRepositoryFactoryBean;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = {
    OrganizationRepository.class}, repositoryFactoryBeanClass = EntityGraphJpaRepositoryFactoryBean.class)
public class JpaConfig {

}
