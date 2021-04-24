import 'package:mrapi/api.dart';

class PortfolioGroup {
  Portfolio? portfolio;
  Group group;

  PortfolioGroup(this.portfolio, this.group);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PortfolioGroup &&
          runtimeType == other.runtimeType &&
          portfolio == other.portfolio &&
          group == other.group;

  @override
  int get hashCode => portfolio.hashCode ^ group.hashCode;

  @override
  String toString() {
    return 'portfolioGroup: (portfolio: ${portfolio?.toString()}, group: ${group?.toString()}';
  }
}
