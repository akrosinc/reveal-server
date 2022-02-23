package com.revealprecision.revealserver.api.v1.facade.dto.response;


import java.io.Serializable;
import org.apache.james.mime4j.field.datetime.DateTime;


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

  public Photo() {

  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public DateTime getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(DateTime dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public int getResourceId() {
    return resourceId;
  }

  public void setResourceId(int resourceId) {
    this.resourceId = resourceId;
  }
}
