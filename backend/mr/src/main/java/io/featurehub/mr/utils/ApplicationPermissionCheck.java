package io.featurehub.mr.utils;

import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Person;
import org.jetbrains.annotations.NotNull;

public class ApplicationPermissionCheck {
  @NotNull
  private final Person current;
  @NotNull
  private final Application app;

  public ApplicationPermissionCheck(@NotNull Person current, @NotNull Application app) {
    this.current = current;
    this.app = app;
  }

  public @NotNull Person getCurrent() {
    return current;
  }

  public @NotNull Application getApp() {
    return app;
  }
}
