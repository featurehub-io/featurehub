import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/percentage_utils.dart';

String generateTooltipMessage(
    RolloutStrategy? rolloutStrategy, AppLocalizations l10n) {
  if (rolloutStrategy == null) {
    return '';
  }

  var percentageMessage = '';
  var userKeyMessage = '';
  var countryNameMessage = '';
  var platformNameMessage = '';
  var deviceNameMessage = '';
  var versionNameMessage = '';
  var customNameMessage = '';

  if (rolloutStrategy.percentage != null) {
    percentageMessage = '${l10n.tooltipPercentage(rolloutStrategy.percentageText)}\n';
  }

  final attrs = rolloutStrategy.attributes ?? [];
  if (attrs.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.userkey.name)) {
    userKeyMessage = '${l10n.tooltipUserKey}\n';
  }

  if (attrs.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.country.name)) {
    countryNameMessage = '${l10n.tooltipCountry}\n';
  }

  if (attrs.any((rsa) =>
      rsa.fieldName == StrategyAttributeWellKnownNames.platform.name)) {
    platformNameMessage = '${l10n.tooltipPlatform}\n';
  }

  if (attrs.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.device.name)) {
    deviceNameMessage = '${l10n.tooltipDevice}\n';
  }

  if (attrs.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.version.name)) {
    versionNameMessage = '${l10n.tooltipVersion}\n';
  }

  if (attrs.any((rsa) => StrategyAttributeWellKnownNames.values
      .every((value) => rsa.fieldName != value.name))) {
    customNameMessage = '${l10n.tooltipCustom}\n';
  }

  return '\n\n-- ${l10n.tooltipAppliedRules} -- \n$percentageMessage$userKeyMessage$countryNameMessage$platformNameMessage$deviceNameMessage$versionNameMessage$customNameMessage';
}
