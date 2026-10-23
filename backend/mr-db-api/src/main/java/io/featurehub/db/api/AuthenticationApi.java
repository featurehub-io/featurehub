package io.featurehub.db.api;

import io.featurehub.mr.model.Person;

import java.time.Instant;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AuthenticationApi {
  Person login(@NotNull String email, @NotNull String password);

  Person register(@Nullable String name, @NotNull String email, @Nullable String password, Opts opts);

  /**
   * in case of forgotten password admin resets the password and sets a reset token which can be used in email.
   *
   * @param password
   * @param changedBy
   * @return
   */
  Person resetPassword(@NotNull UUID id, @NotNull String password, @NotNull UUID changedBy, boolean reactivate);

  /**
   * in case of forgotten password user required to replace temp password after admin has reset it for a user
   * @param id
   * @param password
   * @return
   */
  Person replaceTemporaryPassword(@NotNull UUID id, @NotNull String password);


  /**
   * happens when a user wants to change his password
   * @param id
   * @param oldPassword
   * @param newPassword
   * @return
   */
  Person changePassword(@NotNull UUID id, @NotNull String oldPassword, @NotNull String newPassword);

  Person getPersonByToken(@NotNull String token);

  /**
   * This causes the email address to be issued with a new registration token and a new expiry and that passed back.
   * Valid tokens are reset. User's with no token's are ignored. User's who do not exist are ignored.
   *
   * @param email
   * @return - the token or null if the user does not exist or has no token.
   */
  String resetExpiredRegistrationToken(String email);

  /**
   * Update when the user was last authenticated to now
   *
   * @param id - a person id
   */
  void updateLastAuthenticated(@NotNull UUID id);

  /**
   * A person has successfully authenticated via an SSO provider (OAuth2/SAML). If they are flagged as requiring a
   * password reset (e.g. they were deleted and re-added), that flag would otherwise reject every SSO session they
   * create, so it is cleared. If password login is still enabled, their password is replaced with a long random
   * one so any temporary password can no longer be used to log in.
   *
   * @param id - a person id
   * @param scramblePassword - true if password login is enabled and the password should be replaced
   * @return true if the flag was set and has been cleared
   */
  boolean clearPasswordResetAfterSsoLogin(@NotNull UUID id, boolean scramblePassword);
}
