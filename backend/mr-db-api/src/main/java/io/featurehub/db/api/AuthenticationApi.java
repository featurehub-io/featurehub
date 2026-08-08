package io.featurehub.db.api;

import io.featurehub.mr.model.Person;

import java.time.Instant;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Note on password complexity: the methods here that <em>set</em> a password (register,
 * resetPassword, replaceTemporaryPassword, changePassword) do NOT apply
 * {@link io.featurehub.db.password.PasswordPolicy}. Enforcement lives in the MR resource layer
 * (AuthResource, SetupResource) because that is where the HTTP 400 and its violation report are
 * produced. Any new caller of these methods that accepts a password from a user is responsible for
 * validating it first.
 *
 * <p>Deliberately not enforced here: {@link #login} re-hashes the stored password when the
 * algorithm is out of date, and that write must never be policy checked or every legacy user with a
 * non-compliant password would be locked out on their next sign in.
 */
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
}
