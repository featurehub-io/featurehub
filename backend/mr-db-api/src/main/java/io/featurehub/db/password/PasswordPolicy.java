package io.featurehub.db.password;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The password complexity policy for this deployment.
 *
 * <p>This is only ever consulted when a password is being <em>set</em>. It is deliberately not
 * consulted when an existing password is presented for verification (login, or the old password on
 * a change request) - no FeatureHub release has ever validated password length, so stored passwords
 * may violate this policy, and their owners must still be able to sign in and change them.
 *
 * <p>Enforcement lives in the MR resource layer, not here and not in the persistence layer. See
 * the note on {@code AuthenticationApi}.
 */
public class PasswordPolicy {
  private static final Logger log = LoggerFactory.getLogger(PasswordPolicy.class);

  /**
   * The maximum length declared on the password-setting fields in mr-api.yaml. Generated model
   * validation rejects anything longer before our code runs, so a policy maximum above this would
   * be unreachable - the caller would get a generic validation error instead of a MAX_LENGTH
   * violation. Raising this requires raising those schema caps first.
   */
  public static final int TRANSPORT_MAX_LENGTH = 40;

  public static final int DEFAULT_MIN_LENGTH = 8;
  public static final int DEFAULT_MAX_LENGTH = 40;
  public static final boolean DEFAULT_REQUIRE_UPPERCASE = true;
  public static final boolean DEFAULT_REQUIRE_LOWERCASE = true;
  public static final boolean DEFAULT_REQUIRE_NUMERIC = true;
  public static final boolean DEFAULT_REQUIRE_SYMBOL = false;

  @ConfigKey("auth.password.min-length")
  Integer minLength = DEFAULT_MIN_LENGTH;

  @ConfigKey("auth.password.max-length")
  Integer maxLength = DEFAULT_MAX_LENGTH;

  @ConfigKey("auth.password.require-uppercase")
  Boolean requireUppercase = DEFAULT_REQUIRE_UPPERCASE;

  @ConfigKey("auth.password.require-lowercase")
  Boolean requireLowercase = DEFAULT_REQUIRE_LOWERCASE;

  @ConfigKey("auth.password.require-numeric")
  Boolean requireNumeric = DEFAULT_REQUIRE_NUMERIC;

  @ConfigKey("auth.password.require-symbol")
  Boolean requireSymbol = DEFAULT_REQUIRE_SYMBOL;

  public PasswordPolicy() {
    DeclaredConfigResolver.resolve(this);

    reconcileConfiguration();
  }

  /**
   * For tests and for callers that need a policy independent of deployment configuration.
   */
  public PasswordPolicy(int minLength, int maxLength, boolean requireUppercase, boolean requireLowercase,
                        boolean requireNumeric, boolean requireSymbol) {
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.requireUppercase = requireUppercase;
    this.requireLowercase = requireLowercase;
    this.requireNumeric = requireNumeric;
    this.requireSymbol = requireSymbol;

    reconcileConfiguration();
  }

  /**
   * Guards the two misconfigurations that would otherwise be discovered by a locked out user.
   */
  private void reconcileConfiguration() {
    if (maxLength > TRANSPORT_MAX_LENGTH) {
      log.error(
        "password: auth.password.max-length is {} but passwords longer than {} characters are rejected before they " +
          "reach this policy. Clamping to {}. To allow longer passwords the maxLength on the password fields in " +
          "mr-api.yaml must be raised first.", maxLength, TRANSPORT_MAX_LENGTH, TRANSPORT_MAX_LENGTH);

      maxLength = TRANSPORT_MAX_LENGTH;
    }

    if (minLength > maxLength) {
      log.error(
        "password: auth.password.min-length ({}) is greater than auth.password.max-length ({}), which would reject " +
          "every password including the site admin's. Falling back to the default policy.", minLength, maxLength);

      minLength = DEFAULT_MIN_LENGTH;
      maxLength = DEFAULT_MAX_LENGTH;
      requireUppercase = DEFAULT_REQUIRE_UPPERCASE;
      requireLowercase = DEFAULT_REQUIRE_LOWERCASE;
      requireNumeric = DEFAULT_REQUIRE_NUMERIC;
      requireSymbol = DEFAULT_REQUIRE_SYMBOL;
    }
  }

  /**
   * @param password the password about to be set, as submitted
   * @throws PasswordPolicyViolationException listing every rule the password violates
   */
  public void validate(String password) {
    // PasswordSalter trims before hashing, so validate what will actually be stored - otherwise
    // padding whitespace could satisfy a length rule and then be discarded.
    final String candidate = password == null ? "" : password.trim();

    final List<PasswordPolicyRule> violations = new ArrayList<>();

    final int length = candidate.codePointCount(0, candidate.length());

    if (length < minLength) {
      violations.add(PasswordPolicyRule.MIN_LENGTH);
    }

    if (length > maxLength) {
      violations.add(PasswordPolicyRule.MAX_LENGTH);
    }

    if (requireUppercase && candidate.codePoints().noneMatch(Character::isUpperCase)) {
      violations.add(PasswordPolicyRule.REQUIRE_UPPERCASE);
    }

    if (requireLowercase && candidate.codePoints().noneMatch(Character::isLowerCase)) {
      violations.add(PasswordPolicyRule.REQUIRE_LOWERCASE);
    }

    if (requireNumeric && candidate.codePoints().noneMatch(Character::isDigit)) {
      violations.add(PasswordPolicyRule.REQUIRE_NUMERIC);
    }

    if (requireSymbol && candidate.codePoints().allMatch(Character::isLetterOrDigit)) {
      violations.add(PasswordPolicyRule.REQUIRE_SYMBOL);
    }

    if (!violations.isEmpty()) {
      throw new PasswordPolicyViolationException(violations, minLength, maxLength);
    }
  }

  public int getMinLength() {
    return minLength;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public boolean isRequireUppercase() {
    return requireUppercase;
  }

  public boolean isRequireLowercase() {
    return requireLowercase;
  }

  public boolean isRequireNumeric() {
    return requireNumeric;
  }

  public boolean isRequireSymbol() {
    return requireSymbol;
  }
}
