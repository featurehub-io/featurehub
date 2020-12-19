import 'package:mrapi/api.dart';

String transformRolloutStrategyTypeFieldToString(RolloutStrategyFieldType dropDownStringItem) {
  switch(dropDownStringItem) {

    case RolloutStrategyFieldType.STRING:
      return 'string';
      break;
    case RolloutStrategyFieldType.SEMANTIC_VERSION:
      return 'semantic version';
      break;
    case RolloutStrategyFieldType.NUMBER:
      return 'number';
      break;
    case RolloutStrategyFieldType.DATE:
      return 'date';
      break;
    case RolloutStrategyFieldType.DATETIME:
      return 'date-time';
      break;
    case RolloutStrategyFieldType.BOOLEAN:
      return 'boolean';
      break;
    case RolloutStrategyFieldType.IP_ADDRESS:
      return 'ip address';
      break;
  }
  return '';
}
