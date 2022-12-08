import 'package:mrapi/api.dart';

String transformStrategyAttributeConditionalValueToString(
    RolloutStrategyAttributeConditional dropDownStringItem) {
  switch (dropDownStringItem) {
    case RolloutStrategyAttributeConditional.EQUALS:
      return 'equals';
    case RolloutStrategyAttributeConditional.NOT_EQUALS:
      return 'not equals';
    case RolloutStrategyAttributeConditional.ENDS_WITH:
      return 'ends with';
    case RolloutStrategyAttributeConditional.STARTS_WITH:
      return 'starts with';
    case RolloutStrategyAttributeConditional.GREATER:
      return 'greater';
    case RolloutStrategyAttributeConditional.GREATER_EQUALS:
      return 'greater or equals';
    case RolloutStrategyAttributeConditional.LESS:
      return 'less';
    case RolloutStrategyAttributeConditional.LESS_EQUALS:
      return 'less or equals';
    case RolloutStrategyAttributeConditional.INCLUDES:
      return 'includes substring';
    case RolloutStrategyAttributeConditional.EXCLUDES:
      return 'excludes substring';
    case RolloutStrategyAttributeConditional.REGEX:
      return 'regex';
  }
}
