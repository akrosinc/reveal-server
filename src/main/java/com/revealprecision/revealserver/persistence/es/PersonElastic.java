package com.revealprecision.revealserver.persistence.es;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonElastic {

  private UUID identifier;
  private boolean active;
  private String nameUse;
  private String nameText;
  private String nameFamily;
  private String nameGiven;
  private String namePrefix;
  private String nameSuffix;
  private String gender;
  private Date birthDate;
  private Date deathDate;
  private boolean birthDateApprox;
  private boolean deathDateApprox;
  private List<PersonMetadataElastic> metadata;


}
