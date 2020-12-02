import 'package:mrapi/api.dart';

extension MapNamesForRolloutStrategyViolations on RolloutStrategyViolationType {
  String toDescription() {
    switch (this) {
      case RolloutStrategyViolationType.noName:
        return 'Strategy name is required';
      case RolloutStrategyViolationType.nameTooLong:
        return 'Strategy name is too long';
      case RolloutStrategyViolationType.emptyMatchCriteria:
        return 'You have not provided any rules to match against, please add a rule';
      case RolloutStrategyViolationType.negativePercentage:
        return 'Percentage cannot be a negative number';
      case RolloutStrategyViolationType.percentageOver100Percent:
        return 'The total percentage value across all strategies is above 100%, please decrease the percentage rule';
      case RolloutStrategyViolationType.arrayAttributeNoValues:
        return 'Please provide at least one value for this rule';
      case RolloutStrategyViolationType.attrInvalidWellKnownEnum:
        return 'Please select a value for this rule';
      case RolloutStrategyViolationType.attrMissingValue:
        return 'Please select a value for this rule';
      case RolloutStrategyViolationType.attrMissingConditional:
        return 'Please select a matching condition for this rule';
      case RolloutStrategyViolationType.attrMissingFieldName:
        return 'Please enter the rule name';
      case RolloutStrategyViolationType.attrMissingFieldType:
        return 'Please select a value type for this rule';
      case RolloutStrategyViolationType.attrValNotSemanticVersion:
        return 'Please provide a valid semantic version';
      case RolloutStrategyViolationType.attrValNotNumber:
        return 'Please provide a valid number';
      case RolloutStrategyViolationType.attrValNotDate:
        return 'Please provide a valid date in YYYY-MM-DD format';
      case RolloutStrategyViolationType.attrValNotDateTime:
        return 'Please provide a valid date and time in YYYY-MM-DDTHH:MM:SS format';
      case RolloutStrategyViolationType.attrValNotCidr:
        return 'Please provide a valid IP or CIDR address';
      case RolloutStrategyViolationType.attrUnknownFailure:
        return 'There was an unknown strategy validation error';
    }
  }
}
