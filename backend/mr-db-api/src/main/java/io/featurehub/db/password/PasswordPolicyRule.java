package io.featurehub.db.password;

/**
 * The individual rules a password can violate. These are reported back to clients so they can
 * render localised guidance - the server never sends a human readable message describing a
 * violation, because the admin console is localised and cannot translate one.
 */
public enum PasswordPolicyRule {
  MIN_LENGTH,
  MAX_LENGTH,
  REQUIRE_UPPERCASE,
  REQUIRE_LOWERCASE,
  REQUIRE_NUMERIC,
  REQUIRE_SYMBOL
}
