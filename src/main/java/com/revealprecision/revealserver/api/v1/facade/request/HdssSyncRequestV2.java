package com.revealprecision.revealserver.api.v1.facade.request;

import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class HdssSyncRequestV2 implements Serializable  {

  private String userId;

  private long serverVersion;

  private int batchSize;
}
