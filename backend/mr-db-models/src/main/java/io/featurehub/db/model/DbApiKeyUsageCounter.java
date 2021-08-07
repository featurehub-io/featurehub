package io.featurehub.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fh_apikey_usage")
public class DbApiKeyUsageCounter extends DbBase {
  @ManyToOne(optional = false)
  @Column
  private DbServiceAccountEnvironment apiKey;

  @Column
  private Instant whenPeriodStarts;

  @Column
  private long usageCounter;

  public DbServiceAccountEnvironment getApiKey() {
    return apiKey;
  }

  public void setApiKey(DbServiceAccountEnvironment apiKey) {
    this.apiKey = apiKey;
  }

  public Instant getWhenPeriodStarts() {
    return whenPeriodStarts;
  }

  public void setWhenPeriodStarts(Instant whenPeriodStarts) {
    this.whenPeriodStarts = whenPeriodStarts;
  }

  public long getUsageCounter() {
    return usageCounter;
  }

  public void setUsageCounter(long usageCounter) {
    this.usageCounter = usageCounter;
  }
}
