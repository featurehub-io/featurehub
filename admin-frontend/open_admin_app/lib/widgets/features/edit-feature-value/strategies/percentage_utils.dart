import 'package:mrapi/api.dart';

double _maxPercentage = 1000000.0;
double _percentageMultiplier = _maxPercentage / 100.0;

String percentageText(int? percentage) {
  return percentage == null ? '' : (percentage / _percentageMultiplier).toString();
}

int? percentageFromText(String p) {
  return p.trim().isEmpty ? null : (double.parse(p) * _percentageMultiplier).round();
}

extension RolloutStrategyExtensions on RolloutStrategy {
  double get maxPercentage => _maxPercentage;
  double get percentageMultiplier => _percentageMultiplier;

  set percentageFromText(String p) => percentage = p.trim().isEmpty
      ? null
      : (double.parse(p) * percentageMultiplier).round();

  String get percentageText =>
      percentage == null ? '' : (percentage! / percentageMultiplier).toString();
}
