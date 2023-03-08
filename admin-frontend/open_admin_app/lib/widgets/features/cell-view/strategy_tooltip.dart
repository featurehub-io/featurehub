import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/percentage_utils.dart';

String generateTooltipMessage(RolloutStrategy? rolloutStrategy) {
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
    percentageMessage = 'Percentage: ${rolloutStrategy.percentageText}%\n';
  }

  if (rolloutStrategy.attributes.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.userkey.name)) {
    userKeyMessage = 'User key\n';
  }

  if (rolloutStrategy.attributes.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.country.name)) {
    countryNameMessage = 'Country\n';
  }

  if (rolloutStrategy.attributes.any((rsa) =>
      rsa.fieldName == StrategyAttributeWellKnownNames.platform.name)) {
    platformNameMessage = 'Platform\n';
  }

  if (rolloutStrategy.attributes.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.device.name)) {
    deviceNameMessage = 'Device\n';
  }

  if (rolloutStrategy.attributes.any(
      (rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.version.name)) {
    versionNameMessage = 'Version\n';
  }

  if (rolloutStrategy.attributes.any((rsa) => StrategyAttributeWellKnownNames
      .values
      .every((value) => rsa.fieldName != value.name))) {
    customNameMessage = 'Custom\n';
  }

  var finalString =
      '\n\n-- Applied rules -- \n$percentageMessage$userKeyMessage$countryNameMessage$platformNameMessage$deviceNameMessage$versionNameMessage$customNameMessage';
  return finalString;
}
