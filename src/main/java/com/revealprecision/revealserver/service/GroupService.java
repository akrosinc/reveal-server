package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.Group;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;
//    private final ProducerService producerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroupService(GroupRepository groupRepository
//            , ProducerService producerService
            , ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
//        this.producerService = producerService;
        this.objectMapper = objectMapper;
    }
    public Page<Group> getGroups(Integer pageNumber, Integer pageSize){
        return groupRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }

    public Group createGroup(Group group) {
        Group save = groupRepository.save(group);
        logger.info("Plan saved to database as {}", group);
        return save;
    }

    public Group getGroupByIdentifier(UUID groupIdentifier) {
        return groupRepository.findByIdentifier(groupIdentifier);
    }
}