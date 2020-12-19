import 'package:mrapi/api.dart';

extension RolloutStrategyExtensions on RolloutStrategy {
  double get maxPercentage => 1000000.0;
  double get percentageMultiplier => maxPercentage / 100.0;

  set percentageFromText(String p) => percentage = p.trim().isEmpty
      ? null
      : (double.parse(p) * percentageMultiplier).round();

  String get percentageText =>
      percentage == null ? '' : (percentage / percentageMultiplier).toString();
}
