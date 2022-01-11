package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.GroupTypeEnum;
import lombok.Data;

@Data
public class PersonRequest {

    String name;
    GroupTypeEnum type;

//    UUID locationIdentifier;
}
