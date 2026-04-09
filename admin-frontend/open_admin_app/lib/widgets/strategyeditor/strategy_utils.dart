import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

extension MapNamesForRolloutStrategyViolations on RolloutStrategyViolationType {
  // ignore: missing_return
  String toDescription(AppLocalizations l10n) {
    switch (this) {
      case RolloutStrategyViolationType.noName:
        return l10n.strategyNameRequired;
      case RolloutStrategyViolationType.nameTooLong:
        return l10n.strategyNameTooLong;
      case RolloutStrategyViolationType.emptyMatchCriteria:
        return l10n.strategyEmptyMatchCriteria;
      case RolloutStrategyViolationType.negativePercentage:
        return l10n.strategyNegativePercentage;
      case RolloutStrategyViolationType.percentageOver100Percent:
        return l10n.strategyPercentageOver100;
      case RolloutStrategyViolationType.arrayAttributeNoValues:
        return l10n.strategyArrayAttributeNoValues;
      case RolloutStrategyViolationType.attrInvalidWellKnownEnum:
        return l10n.strategyAttrInvalidWellKnownEnum;
      case RolloutStrategyViolationType.attrMissingValue:
        return l10n.strategyAttrMissingValue;
      case RolloutStrategyViolationType.attrMissingConditional:
        return l10n.strategyAttrMissingConditional;
      case RolloutStrategyViolationType.attrMissingFieldName:
        return l10n.strategyAttrMissingFieldName;
      case RolloutStrategyViolationType.attrMissingFieldType:
        return l10n.strategyAttrMissingFieldType;
      case RolloutStrategyViolationType.attrValNotSemanticVersion:
        return l10n.strategyAttrValNotSemanticVersion;
      case RolloutStrategyViolationType.attrValNotNumber:
        return l10n.strategyAttrValNotNumber;
      case RolloutStrategyViolationType.attrValNotDate:
        return l10n.strategyAttrValNotDate;
      case RolloutStrategyViolationType.attrValNotDateTime:
        return l10n.strategyAttrValNotDateTime;
      case RolloutStrategyViolationType.attrValNotCidr:
        return l10n.strategyAttrValNotCidr;
      case RolloutStrategyViolationType.attrUnknownFailure:
        return l10n.strategyAttrUnknownFailure;
    }
  }
}
