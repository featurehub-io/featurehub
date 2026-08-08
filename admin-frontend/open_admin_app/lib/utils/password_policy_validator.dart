import 'dart:convert';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:openapi_dart_common/openapi.dart';

/// The policy applied when the server has not told us what it is (an old backend, or the setup
/// call failed). These must stay in step with the defaults in the backend's PasswordPolicy - it is
/// better to validate against stale-but-reasonable rules than against nothing, and the server
/// revalidates regardless.
PasswordPolicy get defaultPasswordPolicy => PasswordPolicy(
      minLength: 8,
      maxLength: 40,
      requireUppercase: true,
      requireLowercase: true,
      requireNumeric: true,
      requireSymbol: false,
    );

/// Returns every rule [password] fails, in the same order the rules are presented to the user.
/// Empty means it passes.
///
/// This mirrors the server's validator, including trimming first - the server trims before hashing,
/// so padding whitespace must not count toward the length.
List<PasswordPolicyRule> validatePassword(
    String? password, PasswordPolicy? policy) {
  final p = policy ?? defaultPasswordPolicy;
  final candidate = (password ?? '').trim();
  final runes = candidate.runes.toList();

  final violations = <PasswordPolicyRule>[];

  if (runes.length < p.minLength) {
    violations.add(PasswordPolicyRule.MIN_LENGTH);
  }

  if (runes.length > p.maxLength) {
    violations.add(PasswordPolicyRule.MAX_LENGTH);
  }

  if (p.requireUppercase && !runes.any(_isUppercase)) {
    violations.add(PasswordPolicyRule.REQUIRE_UPPERCASE);
  }

  if (p.requireLowercase && !runes.any(_isLowercase)) {
    violations.add(PasswordPolicyRule.REQUIRE_LOWERCASE);
  }

  if (p.requireNumeric && !runes.any(_isDigit)) {
    violations.add(PasswordPolicyRule.REQUIRE_NUMERIC);
  }

  if (p.requireSymbol && !runes.any((r) => !_isLetter(r) && !_isDigit(r))) {
    violations.add(PasswordPolicyRule.REQUIRE_SYMBOL);
  }

  return violations;
}

/// The rules the active policy actually imposes, in display order, for showing the user what is
/// expected before they start typing.
List<PasswordPolicyRule> activeRules(PasswordPolicy? policy) {
  final p = policy ?? defaultPasswordPolicy;

  return [
    PasswordPolicyRule.MIN_LENGTH,
    if (p.requireUppercase) PasswordPolicyRule.REQUIRE_UPPERCASE,
    if (p.requireLowercase) PasswordPolicyRule.REQUIRE_LOWERCASE,
    if (p.requireNumeric) PasswordPolicyRule.REQUIRE_NUMERIC,
    if (p.requireSymbol) PasswordPolicyRule.REQUIRE_SYMBOL,
  ];
}

/// Localised description of a single rule. The server sends rules as enums precisely so that this
/// text can be translated here.
String describeRule(
    PasswordPolicyRule rule, PasswordPolicy? policy, AppLocalizations l10n) {
  final p = policy ?? defaultPasswordPolicy;

  switch (rule) {
    case PasswordPolicyRule.MIN_LENGTH:
      return l10n.passwordRuleMinLength(p.minLength);
    case PasswordPolicyRule.MAX_LENGTH:
      return l10n.passwordRuleMaxLength(p.maxLength);
    case PasswordPolicyRule.REQUIRE_UPPERCASE:
      return l10n.passwordRuleUppercase;
    case PasswordPolicyRule.REQUIRE_LOWERCASE:
      return l10n.passwordRuleLowercase;
    case PasswordPolicyRule.REQUIRE_NUMERIC:
      return l10n.passwordRuleNumeric;
    case PasswordPolicyRule.REQUIRE_SYMBOL:
      return l10n.passwordRuleSymbol;
  }
}

/// A single line summarising everything wrong with the password, for use as a form field's
/// validator result. Null when the password is acceptable.
String? passwordValidationMessage(
    String? password, PasswordPolicy? policy, AppLocalizations l10n) {
  final violations = validatePassword(password, policy);

  if (violations.isEmpty) return null;

  return violations.map((r) => describeRule(r, policy, l10n)).join('\n');
}

/// Pulls the violated rules out of a failed API call, or null if [e] was not a password policy
/// rejection. The server sends a PasswordPolicyViolationReport with a 400 on every password-setting
/// endpoint.
List<PasswordPolicyRule>? passwordPolicyRejection(Object e) {
  if (e is! ApiException || e.code != 400 || e.message == null) return null;

  try {
    final report = LocalApiClient.deserialize(
        jsonDecode(e.message!), 'PasswordPolicyViolationReport');

    if (report is PasswordPolicyViolationReport &&
        report.violatedRules.isNotEmpty) {
      return report.violatedRules;
    }
  } catch (_) {
    // a 400 that is not a policy report - some other bad request. Let the caller handle it.
  }

  return null;
}

bool _isUppercase(int rune) {
  final s = String.fromCharCode(rune);
  return s.toLowerCase() != s;
}

bool _isLowercase(int rune) {
  final s = String.fromCharCode(rune);
  return s.toUpperCase() != s;
}

bool _isDigit(int rune) => rune >= 0x30 && rune <= 0x39;

bool _isLetter(int rune) {
  final s = String.fromCharCode(rune);
  return s.toLowerCase() != s.toUpperCase();
}
