package com.revealprecision.revealserver.persistence.domain;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PersonGroupKey implements Serializable {
  @Column(name = "person_identifier")
  UUID personIdentifier;
  @Column(name = "group_identifier")
  UUID groupIdentifier;
}
