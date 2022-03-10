package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.facade.models.UserFacadeResponse;
import com.revealprecision.revealserver.persistence.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserFacadeResponseFactory {

  public static UserFacadeResponse fromEntity(User user){
    UserFacadeResponse userFacadeResponse = UserFacadeResponse.builder()
        .baseEntityId(user.getIdentifier().toString())
        .username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName())
        .build();
    return userFacadeResponse;
  }

}
