import 'package:mrapi/api.dart';

String transformStrategyAttributeConditionalValueToString(RolloutStrategyAttributeConditional dropDownStringItem) {
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
      break;
    case RolloutStrategyAttributeConditional.GREATER_EQUALS:
      return 'greater or equals';
      break;
    case RolloutStrategyAttributeConditional.LESS:
      return 'less';
      break;
    case RolloutStrategyAttributeConditional.LESS_EQUALS:
      return 'less or equals';
      break;
    case RolloutStrategyAttributeConditional.INCLUDES:
      return 'includes substring';
      break;
    case RolloutStrategyAttributeConditional.EXCLUDES:
      return 'excludes substring';
      break;
    case RolloutStrategyAttributeConditional.REGEX:
      return 'regex';
      break;
  }

  return '';
}


