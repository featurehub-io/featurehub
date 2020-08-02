package io.featurehub.db.api;

import io.featurehub.mr.model.Person;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface AuthenticationApi {
  Person login(@NotNull String email, @NotNull String password);

  /**
   * @param name
   * @param email
   * @param password
   * @return
   */
  Person register(@NotNull String name, @NotNull String email, @NotNull String password);

  /**
   * in case of forgotten password admin resets the password and sets a reset token which can be used in email.
   *
   * @param password
   * @param changedBy
   * @return
   */
  Person resetPassword(@NotNull String id, @NotNull String password, @NotNull String changedBy, boolean reactivate);

  /**
   * in case of forgotten password user required to replace temp password after admin has reset it for a user
   * @param id
   * @param password
   * @return
   */
  Person replaceTemporaryPassword(@NotNull String id, @NotNull String password);


  /**
   * happens when a user wants to change his password
   * @param id
   * @param oldPassword
   * @param newPassword
   * @return
   */
  Person changePassword(@NotNull String id, @NotNull String oldPassword, @NotNull String newPassword);

  Person getPersonByToken(String token);

  DBLoginSession findSession(String token);

  DBLoginSession createSession(DBLoginSession session);

  void invalidateSession(String sessionToken);

  /**
   * This causes the email address to be issued with a new registration token and a new expiry and that passed back.
   * Valid tokens are reset. User's with no token's are ignored. User's who do not exist are ignored.
   *
   * @param email
   * @return - the token or null if the user does not exist or has no token.
   */
  String resetExpiredRegistrationToken(String email);
}
