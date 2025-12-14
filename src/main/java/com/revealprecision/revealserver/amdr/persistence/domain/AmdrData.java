package com.revealprecision.revealserver.amdr.persistence.domain;

import com.revealprecision.revealserver.amdr.model.KeyValue;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(schema = "amdr")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Setter @Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AmdrData {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID locationId;

  private String type;

  private String overallValue;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<KeyValue> data;

  private LocalDateTime datetime;

}
