package io.featurehub.db.services;

import io.ebean.Database;
import io.featurehub.db.api.UserStateApi;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbUserState;
import io.featurehub.db.model.UserState;
import io.featurehub.db.model.query.QDbUserState;
import io.featurehub.mr.model.HiddenEnvironments;
import io.featurehub.mr.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class UserStateSqlApi implements UserStateApi {
  private static final Logger log = LoggerFactory.getLogger(UserStateSqlApi.class);
  private final Conversions conversions;
  private final Database database;

  @Inject
  public UserStateSqlApi(Conversions conversions, Database database) {
    this.conversions = conversions;
    this.database = database;
  }

  private QDbUserState userStateFinder(Person person, UUID appUid, UserState userState) {
    return new QDbUserState()
      .person.id.eq(Conversions.ifUuid(person.getId().getId()))
      .application.id.eq(appUid)
      .userState.eq(userState);
  }

  @Override
  public HiddenEnvironments getHiddenEnvironments(Person person, String appId) {
    final UUID appUid = Conversions.ifUuid(appId);
    if (appUid != null) {
      final DbUserState features = userStateFinder(person, appUid, UserState.HIDDEN_FEATURES).findOne();

      if (features != null) {
        return Conversions.readJsonValue(features.getData(), HiddenEnvironments.class);
      }
    }

    return null;
  }

  @Override
  public void saveHiddenEnvironments(Person currentPerson, HiddenEnvironments environments, String appId) {
    final DbApplication application = conversions.uuidApplication(appId);
    final DbPerson person = conversions.uuidPerson(currentPerson);

    if (application == null || person == null) {
      log.warn("Attempt made to save user state with invalid person or application.");
      return;
    }

    if (environments == null || environments.getEnvironmentIds() == null || environments.getEnvironmentIds().isEmpty()) {
      userStateFinder(currentPerson, application.getId(), UserState.HIDDEN_FEATURES).delete();
    }

    DbUserState features =
        userStateFinder(currentPerson, application.getId(), UserState.HIDDEN_FEATURES).findOne();

    if (features == null) {
      features = new DbUserState.Builder()
          .application(application)
          .person(person)
          .build();

      if (features.getApplication() == null || features.getPerson() == null ) {
        return;
      }
    }

    features.setData(Conversions.valueToJsonString(environments));

    if (features.getData() != null) {
      database.save(features);
    }
  }
}
