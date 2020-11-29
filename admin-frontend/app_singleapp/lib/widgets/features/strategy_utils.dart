import 'package:mrapi/api.dart';

extension MapNamesForRolloutStrategyViolations on RolloutStrategyViolationType {
  String toDescription() {
    switch (this) {
      case RolloutStrategyViolationType.noName:
        return 'No name provided for the rollout strategy, one is required';
      case RolloutStrategyViolationType.nameTooLong:
        return 'Name for strategy is too long';
      case RolloutStrategyViolationType.emptyMatchCriteria:
        return 'You have not provided any criteria to match against, no percentage or attributes.';
      case RolloutStrategyViolationType.negativePercentage:
        return 'You are trying to match against a negative percentage';
      case RolloutStrategyViolationType.percentageOver100Percent:
        return 'The total percentage with this strategy now exceeds 100%, please decrease to within 100%.';
      case RolloutStrategyViolationType.arrayAttributeNoValues:
        return 'This attribute is an array but has no values';
      case RolloutStrategyViolationType.attrInvalidWellKnownEnum:
        return 'The value for this attribute is missing';
      case RolloutStrategyViolationType.attrMissingValue:
        return 'The value for this attribute is missing';
      case RolloutStrategyViolationType.attrMissingConditional:
        return 'This attribute is missing the condition.';
      case RolloutStrategyViolationType.attrMissingFieldName:
        return 'This custom attribute is missing the field name';
      case RolloutStrategyViolationType.attrMissingFieldType:
        return 'This attribute is missing the field type';
      case RolloutStrategyViolationType.attrValNotSemanticVersion:
        return 'One of the value(s) is not a valid semantic version.';
      case RolloutStrategyViolationType.attrValNotNumber:
        return 'Expected a number but one of the value(s) is not a number';
      case RolloutStrategyViolationType.attrValNotDate:
        return 'Expected a Date in YYYY-MM-DD format and value(s) were invalid.';
      case RolloutStrategyViolationType.attrValNotDateTime:
        return 'Expected a DateTime in YYYY-MM-DDTHH:MM:SS format and value(s) were invalid.';
      case RolloutStrategyViolationType.attrValNotCidr:
        return 'One of the value(s) is not a value IP or CIDR address';
      case RolloutStrategyViolationType.attrUnknownFailure:
        return 'There was an unknown failure validating your strategy';
    }
  }
}
