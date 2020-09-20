import 'package:mrapi/api.dart';

extension RolloutStrategyExtensions on RolloutStrategy {
  double get maxPercentage => 1000000.0;
  double get percentageMultiplier => maxPercentage / 100.0;

  set percentageFromText(String p) =>
      percentage = (double.parse(p) * percentageMultiplier).round();

  String get percentageText => this.percentage == null
      ? ''
      : (percentage / percentageMultiplier).toString();
}
