package com.revealprecision.revealserver.enums;

/*
As defined by https://www.hl7.org/fhir/datatypes.html#HumanName, we are not making all the available options available for simplicity reasons.
Usual, Official, Temp, Nickname, Anonymous, Old, Maiden
*/

import java.util.HashMap;
import java.util.Map;

public enum NameUseEnum {
    USUAL("usual"),
    OFFICIAL("official"),
    TEMP("temp"),
    NICKNAME("nickname"),
    ANONYMOUS("anonymous"),
    OLD("old"),
    MAIDEN("maiden");


    private final String value;
    NameUseEnum(String value){
        this.value =  value;
    }

    private static final Map<String, NameUseEnum> BY_VALUE = new HashMap<>();

    static {
        for (NameUseEnum e: values()) {
            BY_VALUE.put(e.value, e);
        }
    }

    public static NameUseEnum getEnum(String value){
        return BY_VALUE.get(value);
    }

    }
