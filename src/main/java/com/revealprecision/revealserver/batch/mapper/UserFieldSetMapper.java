package com.revealprecision.revealserver.batch.mapper;

import com.revealprecision.revealserver.batch.FieldSetConverter;
import com.revealprecision.revealserver.batch.dto.UserBatchDTO;
import java.util.Set;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

@Component
public class UserFieldSetMapper implements FieldSetMapper<UserBatchDTO> {

  @Autowired
  FieldSetConverter converter;

  @Override
  public UserBatchDTO mapFieldSet(FieldSet fieldSet) throws BindException {
    try {
      String email = (String) converter.convert(fieldSet.readString("email"), String.class);
      return UserBatchDTO.builder()
          .userName((String) converter.convert(fieldSet.readString("userName"), String.class))
          .firstName((String) converter.convert(fieldSet.readString("firstName"), String.class))
          .lastName((String) converter.convert(fieldSet.readString("lastName"), String.class))
          .email(email.isBlank() ? null : email)
          .password((String) converter.convert(fieldSet.readString("password"), String.class))
          .tempPassword(
              (Boolean) converter.convert(fieldSet.readString("tempPassword"), Boolean.class))
          .securityGroups(
              (Set<String>) converter.convert(fieldSet.readString("securityGroups"), Set.class))
          .organizations(
              (Set<String>) converter.convert(fieldSet.readString("organizations"), Set.class))
          .build();
    } catch (IllegalAccessException e) {
      return null;
    }
  }
}
