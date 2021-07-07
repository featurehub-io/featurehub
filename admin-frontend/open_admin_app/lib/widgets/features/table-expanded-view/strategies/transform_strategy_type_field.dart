import 'package:mrapi/api.dart';

String transformRolloutStrategyTypeFieldToString(
    RolloutStrategyFieldType dropDownStringItem) {
  switch (dropDownStringItem) {
    case RolloutStrategyFieldType.STRING:
      return 'string';
    case RolloutStrategyFieldType.SEMANTIC_VERSION:
      return 'semantic version';
    case RolloutStrategyFieldType.NUMBER:
      return 'number';
    case RolloutStrategyFieldType.DATE:
      return 'date';
    case RolloutStrategyFieldType.DATETIME:
      return 'date-time';
    case RolloutStrategyFieldType.BOOLEAN:
      return 'boolean';
    case RolloutStrategyFieldType.IP_ADDRESS:
      return 'ip address';
  }
}
