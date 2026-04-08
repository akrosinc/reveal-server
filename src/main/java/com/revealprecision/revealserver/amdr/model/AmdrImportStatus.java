package com.revealprecision.revealserver.amdr.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AmdrImportStatus implements Serializable {

  String status;

  int count;
}
