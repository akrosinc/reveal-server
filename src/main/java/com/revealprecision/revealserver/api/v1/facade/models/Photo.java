package com.revealprecision.revealserver.api.v1.facade.models;


import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.james.mime4j.field.datetime.DateTime;


@Getter
@Setter
@NoArgsConstructor
public class Photo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String contentType;

  private String language;

  private String uri;

  private int size;

  private String title;

  private String category;

  private DateTime dateCreated;

  private String filePath;

  private int resourceId;
}
