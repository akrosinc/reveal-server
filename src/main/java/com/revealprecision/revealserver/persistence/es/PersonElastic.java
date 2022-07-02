package com.revealprecision.revealserver.persistence.es;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonElastic {

  @Field(type = FieldType.Text)
  private UUID identifier;
  @Field(type = FieldType.Text)
  private boolean active;
  @Field(type = FieldType.Text)
  private String nameUse;
  @Field(type = FieldType.Text)
  private String nameText;
  @Field(type = FieldType.Text)
  private String nameFamily;
  @Field(type = FieldType.Text)
  private String nameGiven;
  @Field(type = FieldType.Text)
  private String namePrefix;
  @Field(type = FieldType.Text)
  private String nameSuffix;
  @Field(type = FieldType.Text)
  private String gender;
  @Field(type = FieldType.Text)
  private Date birthDate;
  @Field(type = FieldType.Date)
  private Date deathDate;
  @Field(type = FieldType.Boolean)
  private boolean birthDateApprox;
  @Field(type = FieldType.Boolean)
  private boolean deathDateApprox;
  @Field(type = FieldType.Flattened)
  private List<PersonMetadataElastic> metadata;


}
