package com.revealprecision.revealserver.persistence.projection;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public interface IdentifierNameProjection{
  UUID getIdentifier();
  String getName();
}
