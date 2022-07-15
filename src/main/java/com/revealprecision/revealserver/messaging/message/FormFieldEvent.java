package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.persistence.domain.AbstractAuditableEntity;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormFieldEvent extends Message {

  private UUID identifier;

  private String name;

  private String display;

  private boolean addToMetadata;

  private String dataType;

  private String formTitle;
}
