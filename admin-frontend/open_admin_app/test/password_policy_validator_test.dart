import 'package:flutter_test/flutter_test.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/password_policy_validator.dart';

/// These mirror the backend's PasswordPolicySpec. If the two drift, the console starts accepting
/// passwords the server rejects (or vice versa), which is exactly the confusing experience the
/// published policy exists to avoid.
void main() {
  PasswordPolicy policy({
    int minLength = 8,
    int maxLength = 40,
    bool requireUppercase = true,
    bool requireLowercase = true,
    bool requireNumeric = true,
    bool requireSymbol = false,
  }) =>
      PasswordPolicy(
        minLength: minLength,
        maxLength: maxLength,
        requireUppercase: requireUppercase,
        requireLowercase: requireLowercase,
        requireNumeric: requireNumeric,
        requireSymbol: requireSymbol,
      );

  test('a compliant password passes the default policy', () {
    expect(validatePassword('Passw0rdX', policy()), isEmpty);
  });

  test('non-ascii letters count toward their character class', () {
    expect(validatePassword('Ünterwegs9', policy()), isEmpty);
  });

  test('every violated rule is reported, not just the first', () {
    final violations = validatePassword('abc', policy());

    expect(violations, contains(PasswordPolicyRule.MIN_LENGTH));
    expect(violations, contains(PasswordPolicyRule.REQUIRE_UPPERCASE));
    expect(violations, contains(PasswordPolicyRule.REQUIRE_NUMERIC));
    expect(violations, isNot(contains(PasswordPolicyRule.REQUIRE_LOWERCASE)));
  });

  test('the password is validated trimmed, matching the server', () {
    expect(validatePassword('   aB1   ', policy()),
        contains(PasswordPolicyRule.MIN_LENGTH));
    expect(validatePassword('        ', policy()),
        contains(PasswordPolicyRule.MIN_LENGTH));
    expect(
        validatePassword(null, policy()), contains(PasswordPolicyRule.MIN_LENGTH));
  });

  test('a relaxed policy accepts a simple password', () {
    expect(
        validatePassword(
            'simple',
            policy(
                minLength: 6,
                requireUppercase: false,
                requireLowercase: false,
                requireNumeric: false)),
        isEmpty);
  });

  test('a required symbol is enforced, and interior whitespace satisfies it', () {
    final p = policy(requireSymbol: true);

    expect(validatePassword('Passw0rdX', p),
        equals([PasswordPolicyRule.REQUIRE_SYMBOL]));
    expect(validatePassword('Passw0rd!', p), isEmpty);
    expect(validatePassword('Passw0rd X', p), isEmpty);
  });

  test('a password over the maximum is reported', () {
    final tooLong = 'aB3${'x' * 38}';

    expect(tooLong.length, 41);
    expect(validatePassword(tooLong, policy()),
        equals([PasswordPolicyRule.MAX_LENGTH]));
  });

  test('with no policy from the server it falls back to the documented defaults',
      () {
    expect(validatePassword('Passw0rdX', null), isEmpty);
    expect(validatePassword('letmein', null),
        contains(PasswordPolicyRule.REQUIRE_UPPERCASE));
  });

  test('only the rules the policy actually imposes are shown to the user', () {
    expect(
        activeRules(policy()),
        equals([
          PasswordPolicyRule.MIN_LENGTH,
          PasswordPolicyRule.REQUIRE_UPPERCASE,
          PasswordPolicyRule.REQUIRE_LOWERCASE,
          PasswordPolicyRule.REQUIRE_NUMERIC,
        ]));

    expect(activeRules(policy(requireSymbol: true)),
        contains(PasswordPolicyRule.REQUIRE_SYMBOL));

    expect(
        activeRules(policy(
            requireUppercase: false,
            requireLowercase: false,
            requireNumeric: false)),
        equals([PasswordPolicyRule.MIN_LENGTH]));
  });
}
