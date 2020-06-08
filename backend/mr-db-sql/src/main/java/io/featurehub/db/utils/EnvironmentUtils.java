package io.featurehub.db.utils;

import io.featurehub.db.model.DbEnvironment;

public class EnvironmentUtils {
  public static Integer walkAndCompare(DbEnvironment env1, DbEnvironment env2) {
    if (env1.getPriorEnvironment() == null) {
      return null;
    }

    // env1's prior environment can't be env2
    if (env2 == null) {
      return null;
    }

    if (env1.getPriorEnvironment() == env2) {
      return 1;
    }

    if (env2.getPriorEnvironment() == env1) {
      return -1;
    }

    return walkAndCompare(env1, env2.getPriorEnvironment());
  }
}
