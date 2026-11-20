package io.featurehub.sse;

import java.util.Date;

public class Todo {
  private String id;
  private String title;
  private Boolean resolved;
  private Date when;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getResolved() {
    return resolved;
  }

  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
  }

  public Date getWhen() {
    return when;
  }

  public void setWhen(Date when) {
    this.when = when;
  }
}
