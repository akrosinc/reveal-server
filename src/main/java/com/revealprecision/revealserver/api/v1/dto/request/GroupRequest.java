package com.revealprecision.revealserver.api.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.revealprecision.revealserver.enums.GroupTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class GroupRequest {

    String name;
    GroupTypeEnum type;

//    UUID locationIdentifier;
}
