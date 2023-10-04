package io.featurehub.db.services

import cd.connect.app.config.ConfigKey
import io.ebean.Database
import io.featurehub.db.api.UserStateApi
import io.featurehub.db.model.DbUserState
import io.featurehub.db.model.UserState
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbUserState
import io.featurehub.mr.model.HiddenEnvironments
import io.featurehub.mr.model.Person
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.lang.Boolean
import java.util.*
import java.util.stream.Collectors

class UserStateSqlApi @Inject constructor(private val conversions: Conversions) :
  UserStateApi {
  @ConfigKey("limit.maxEnvironmentsPerApplication")
  protected var maximumEnvironmentsPerApplication = 1000
  private fun userStateFinder(person: Person, appUid: UUID, userState: UserState): QDbUserState {
    Conversions.nonNullApplicationId(appUid)
    Conversions.nonNullPerson(person)
    return QDbUserState().person.id.eq(person.id!!.id).application.id.eq(appUid).userState.eq(userState)
  }

  override fun getHiddenEnvironments(person: Person, appUid: UUID): HiddenEnvironments? {
    Conversions.nonNullApplicationId(appUid)
    val features = userStateFinder(person, appUid, UserState.HIDDEN_FEATURES).findOne()
    return if (features != null) {
      Conversions.readJsonValue(
        features.data,
        HiddenEnvironments::class.java
      )!!
    } else null
  }

  @Throws(UserStateApi.InvalidUserStateException::class)
  override fun saveHiddenEnvironments(currentPerson: Person, environments: HiddenEnvironments, appId: UUID) {
    val application = conversions.byApplication(appId) ?: return
    val person = conversions.byPerson(currentPerson) ?: return

    if (environments.environmentIds.isEmpty() && (environments.noneSelected !== true)) {
      userStateFinder(currentPerson, application.id, UserState.HIDDEN_FEATURES).delete()
      return
    }

    // too many environments?
    if (environments.environmentIds.size > maximumEnvironmentsPerApplication) {
      throw UserStateApi.InvalidUserStateException("Too many environments.")
    }

    // environment ids that aren't uuids?
    val envIds = environments.environmentIds
      .stream().filter { obj: UUID? -> Objects.nonNull(obj) }.collect(Collectors.toList())
    if (envIds.size != environments.environmentIds.size) {
      throw UserStateApi.InvalidUserStateException("Invalid UUIDs in environments list")
    }

    // environment ids that don't exist?
    if (QDbEnvironment().id.`in`(envIds).findCount() != envIds.size) {
      throw UserStateApi.InvalidUserStateException("Invalid number of environments in environments list")
    }
    var features = userStateFinder(currentPerson, application.id, UserState.HIDDEN_FEATURES).findOne()
    if (features == null) {
      features = DbUserState.Builder()
        .application(application)
        .person(person)
        .userState(UserState.HIDDEN_FEATURES)
        .build()
    }

    features!!.data = Conversions.valueToJsonString(environments)

    if (features.data != null) {
      features.save()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(UserStateSqlApi::class.java)
  }
}
