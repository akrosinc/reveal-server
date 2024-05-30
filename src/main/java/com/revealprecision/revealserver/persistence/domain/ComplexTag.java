package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagDto.TagWithFormulaSymbol;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Setter
@Getter
@Entity
@FieldNameConstants
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ComplexTag {

  @Id
  @GeneratedValue
  private Integer id;

  private String hierarchyId;

  private String hierarchyType;

  private String tagName;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<TagWithFormulaSymbol> tags;

  private String formula;

  private boolean isPublic;

  @OneToMany(mappedBy = "complexTag",cascade = CascadeType.ALL)
  private List<ComplexTagAccGrantsOrganization> complexTagAccGrantsOrganizations;

  @OneToMany(mappedBy = "complexTag",cascade = CascadeType.ALL)
  private List<ComplexTagAccGrantsUser> complexTagAccGrantsUsers;
}
