package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.Index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fh_after_mig_job")
public class DbAfterMigrationJob extends Model {
  @Column(nullable = false)
  @Id
  private int id;
  @Column(nullable = false)
  private String jobName;
  @Column(nullable = false)
  private boolean completed;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }
}
