package io.featurehub.db.model;

import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

@Embeddable
public class DbGroupMemberKey extends Model {
  @Column(name = "fk_person_id")
  private final UUID personId;
  @Column(name = "fk_group_id")
  private final UUID groupId;

  public DbGroupMemberKey(@NotNull UUID personId, @NotNull UUID groupId) {
    this.personId = personId;
    this.groupId = groupId;
  }

  public UUID getPersonId() {
    return personId;
  }

  public UUID getGroupId() {
    return groupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbGroupMemberKey that = (DbGroupMemberKey) o;
    return getPersonId().equals(that.getPersonId()) && getGroupId().equals(that.getGroupId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPersonId(), getGroupId());
  }
}
