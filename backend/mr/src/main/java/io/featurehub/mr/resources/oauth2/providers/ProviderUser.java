package io.featurehub.mr.resources.oauth2.providers;

public class ProviderUser {
  private String email;
  private String name;

  private ProviderUser(Builder builder) {
    setEmail(builder.email);
    setName(builder.name);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static final class Builder {
    private String email;
    private String name;

    public Builder() {
    }

    public Builder email(String val) {
      email = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public ProviderUser build() {
      return new ProviderUser(this);
    }
  }
}
