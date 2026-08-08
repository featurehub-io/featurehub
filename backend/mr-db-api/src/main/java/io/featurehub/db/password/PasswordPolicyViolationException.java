package io.featurehub.db.password;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a password fails {@link PasswordPolicy#validate}. Carries every rule the password
 * violated, not just the first, so the caller can tell the user everything that is wrong in one go.
 *
 * <p>Neither the message nor any field of this exception contains the password or any part of it -
 * this exception is logged and serialised to clients.
 */
public class PasswordPolicyViolationException extends RuntimeException {
  private final List<PasswordPolicyRule> violations;
  private final int minLength;
  private final int maxLength;

  public PasswordPolicyViolationException(List<PasswordPolicyRule> violations, int minLength, int maxLength) {
    super("password rejected by policy: " + violations);

    this.violations = Collections.unmodifiableList(violations);
    this.minLength = minLength;
    this.maxLength = maxLength;
  }

  public List<PasswordPolicyRule> getViolations() {
    return violations;
  }

  public int getMinLength() {
    return minLength;
  }

  public int getMaxLength() {
    return maxLength;
  }
}
