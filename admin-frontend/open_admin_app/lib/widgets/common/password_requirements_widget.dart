import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/password_policy_validator.dart';

/// Shows the active password policy as a live checklist, so the user can see what is expected
/// before they start typing and watch each rule go green as they satisfy it.
///
/// [password] is what the user has typed so far; pass null/empty before they start and every rule
/// renders as simply "not yet met" rather than as an error.
class PasswordRequirementsWidget extends StatelessWidget {
  final PasswordPolicy? policy;
  final String? password;

  /// Rules the *server* rejected. Shown separately - if these appear it means the server and this
  /// client disagree, and the server wins.
  final List<PasswordPolicyRule>? serverRejectedRules;

  const PasswordRequirementsWidget({
    super.key,
    required this.policy,
    this.password,
    this.serverRejectedRules,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);

    final rules = activeRules(policy);
    final unmet = validatePassword(password, policy);
    final started = (password ?? '').isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(l10n.passwordRequirementsTitle,
            style: theme.textTheme.bodySmall
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 4.0),
        ...rules.map((rule) {
          final met = started && !unmet.contains(rule);

          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 1.0),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  met ? Icons.check_circle_outline : Icons.circle_outlined,
                  size: 14.0,
                  color: met ? Colors.green : theme.disabledColor,
                ),
                const SizedBox(width: 6.0),
                Flexible(
                  child: Text(
                    describeRule(rule, policy, l10n),
                    style: theme.textTheme.bodySmall?.copyWith(
                        color: met
                            ? Colors.green
                            : theme.textTheme.bodySmall?.color),
                  ),
                ),
              ],
            ),
          );
        }),
        if (serverRejectedRules != null && serverRejectedRules!.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l10n.passwordRejectedByServer,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error)),
                ...serverRejectedRules!.map((rule) => Text(
                      '• ${describeRule(rule, policy, l10n)}',
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.error),
                    )),
              ],
            ),
          ),
      ],
    );
  }
}
