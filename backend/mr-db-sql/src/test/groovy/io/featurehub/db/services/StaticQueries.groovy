package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.query.QDbApplication
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbGroupMember
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.mr.model.Application
import org.jetbrains.annotations.Nullable

class StaticQueries {
  @CompileStatic
  @Nullable static DbFeatureValue fv(UUID envId, UUID featureId) {
    return new QDbFeatureValue().environment.id.eq(envId).feature.id.eq(featureId).findOne()
  }

  @CompileStatic
  @Nullable static DbEnvironment environment(UUID id) {
    def env = new QDbEnvironment().id.eq(id).findOne()
    if (env != null) {
      env.refresh()
    }
    return env
  }

  @CompileStatic
  @Nullable static DbEnvironment findEnvironment(UUID id) {
    return new QDbEnvironment().id.eq(id).findOne()
  }

  @CompileStatic
  @Nullable static DbApplication findApplication(UUID id) {
    return new QDbApplication().id.eq(id).findOne()
  }

  @CompileStatic
  static DbApplicationRolloutStrategy appStrategy(Application app1, UUID id) {
    return new QDbApplicationRolloutStrategy().id.eq(id).application.id.eq(app1.id).findOne()
  }

  @CompileStatic
  static DbOrganization findOrganization(UUID id) {
    return new QDbOrganization().id.eq(id).findOne()
  }

  @CompileStatic
  static boolean userInGroup(UUID personId, UUID groupId) {
    return new QDbGroupMember().person.id.eq(personId).group.id.eq(groupId).exists()
  }
}
