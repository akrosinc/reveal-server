package com.revealprecision.revealserver.batch;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;

@Service
public class FieldSetConverter {

    public Object convert(String valueToConvert, Class typeOfValueToConvertTo) throws IllegalAccessException {
        if (valueToConvert.equals("\\N")) {
            return null;
        } else if (valueToConvert.isBlank() && typeOfValueToConvertTo.getName().equals("java.lang.Boolean")) {
            return true;
        }

        switch (typeOfValueToConvertTo.getTypeName()) {
            case "java.lang.String": {
                return valueToConvert;
            }
            case "java.util.Set": {
                return new HashSet<>(Arrays.asList(valueToConvert.split(";")));
            }
            case "java.lang.Boolean": {
                return Boolean.valueOf(valueToConvert);
            }
            default:
                throw new IllegalAccessException("Unexpected value: " + typeOfValueToConvertTo.getTypeName());
        }

    }
}
