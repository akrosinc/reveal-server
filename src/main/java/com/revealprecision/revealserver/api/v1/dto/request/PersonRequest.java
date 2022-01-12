package com.revealprecision.revealserver.api.v1.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
public class PersonRequest {

    boolean active;
    Name name;
    Gender gender;
    LocalDate birthDate;
    String[] groups;

    @Data
    @Builder
    public static class Name{
        Use use;
        String text;
        String family;
        String given;
        String prefix;
        String suffix;
    }


    public enum Use{
        usual,
        official,
        temp,
        nickname,
        anonymous,
        old,
        maiden
    }

    public enum Gender{
        male,
        female,
        other,
        unknown
    }
}

