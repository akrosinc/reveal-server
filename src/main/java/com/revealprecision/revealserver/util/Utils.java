package com.revealprecision.revealserver.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {
  public static List<Integer> getYearsFromString(String years){
    if(StringUtils.isBlank(years)){
      return List.of(0);
    }

    try{
        return Arrays.stream(years.split(",")).map(Integer::parseInt)
            .collect(Collectors.toList());
    }
    catch (Exception e){
      return List.of(0);
    }
  }
}
