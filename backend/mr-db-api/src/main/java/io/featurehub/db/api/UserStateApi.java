package io.featurehub.db.api;

import io.featurehub.mr.model.HiddenEnvironments;
import io.featurehub.mr.model.Person;

import java.util.UUID;

public interface UserStateApi {
  class InvalidUserStateException extends Exception {
    public InvalidUserStateException(String message) {
      super(message);
    }
  }
  /**
   * Get the list of environments stashed for this user.
   *
   * @param person
   * @param appId
   * @return
   */
  HiddenEnvironments getHiddenEnvironments(Person person, UUID appId);

  /**
   * It is the responsibility of the caller to assert the person has the rights to the application to save
   * anything. They do not need to worry about the environments themselves as they are not important.
   *
   * @param person
   * @param environments
   * @param appId
   */
  void saveHiddenEnvironments(Person person, HiddenEnvironments environments, UUID appId) throws InvalidUserStateException;
}
