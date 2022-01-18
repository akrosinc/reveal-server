package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.GroupTypeEnum;
import java.util.UUID;
import lombok.Data;

@Data
public class GroupRequest {

    String name;
    GroupTypeEnum type;
    UUID locationIdentifier;
}
