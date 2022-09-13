package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CountryCampaign{

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<AgeGroup> groups;

  private String key;
}