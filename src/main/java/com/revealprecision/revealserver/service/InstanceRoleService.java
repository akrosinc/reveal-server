package com.revealprecision.revealserver.service;


import com.revealprecision.revealserver.enums.InstanceRoleEnum;
import com.revealprecision.revealserver.persistence.domain.InstanceRole;
import com.revealprecision.revealserver.persistence.repository.InstanceRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstanceRoleService {

    private final InstanceRoleRepository instanceRoleRepository;

    public InstanceRole getInstanceAdminRole() {
        return instanceRoleRepository.findByName(InstanceRoleEnum.ADMIN.name())
                .orElseThrow(() ->
                        new IllegalStateException("ADMIN role not configured"));
    }

    public InstanceRole getStandardRole() {
        return instanceRoleRepository.findByName(InstanceRoleEnum.STANDARD.name())
                .orElseThrow(() ->
                        new IllegalStateException("STANDARD role not configured"));
    }
}