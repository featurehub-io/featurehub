package io.featurehub.db.utils;

import io.featurehub.db.model.DbEnvironment;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

  public static void sortEnvironments(List<DbEnvironment> environments) {
    Map<UUID, DbEnvironment> environmentOrderingMap = environments.stream().collect(Collectors.toMap(DbEnvironment::getId, e -> e));

    environments.sort((o1, o2) -> {
      final DbEnvironment env1 = environmentOrderingMap.get(o1.getId());
      final DbEnvironment env2 = environmentOrderingMap.get(o2.getId());

      Integer w = walkAndCompare(env1, env2);
      if (w == null) {
        w = walkAndCompare(env2, env1);
        if (w == null) {
          if (env1.getPriorEnvironment() == null && env2.getPriorEnvironment() == null) {
            return 0;
          }
          if (env1.getPriorEnvironment() != null && env2.getPriorEnvironment() == null) {
            return 1;
          }
          return -1;
        } else {
          return w * -1;
        }
      }

      return w;
    });
  }
}
