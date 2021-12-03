package com.revealprecision.revealserver.exceptions;

import com.revealprecision.revealserver.exceptions.constant.Error;
import org.springframework.data.util.Pair;

public class NotFoundException extends RuntimeException{

    public NotFoundException(String message){
        super(message);
    }

    public NotFoundException(Pair<String, Object> keyValueQuery, Class<?> resourceType){
        super(String.format(Error.RESOURCE_NOT_FOUND,resourceType.getSimpleName(), keyValueQuery.toString()));
    }
}
