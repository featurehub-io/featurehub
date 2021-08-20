package io.featurehub.db.model;

public enum RoleType {
  READ("READ"),

  LOCK("LOCK"),

  UNLOCK("UNLOCK"),

  CHANGE_VALUE("CHANGE_VALUE");

  private String value;

  RoleType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static RoleType fromValue(String text) {
    for (RoleType b : RoleType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + text + "'");
  }
}
