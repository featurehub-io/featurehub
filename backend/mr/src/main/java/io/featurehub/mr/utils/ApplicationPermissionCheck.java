package io.featurehub.mr.utils;

import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Person;

public class ApplicationPermissionCheck {
  private Person current;
  private Application app;

  private ApplicationPermissionCheck(Builder builder) {
    current = builder.current;
    app = builder.app;
  }

  public Person getCurrent() {
    return current;
  }

  public Application getApp() {
    return app;
  }


  public static final class Builder {
    private Person current;
    private Application app;

    public Builder() {
    }

    public Builder current(Person val) {
      current = val;
      return this;
    }

    public Builder app(Application val) {
      app = val;
      return this;
    }

    public ApplicationPermissionCheck build() {
      return new ApplicationPermissionCheck(this);
    }
  }
}
