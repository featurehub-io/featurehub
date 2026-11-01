import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/cell-view/strategy_tooltip.dart';

Widget _wrap(Widget child) => MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: Center(child: child)),
    );

void main() {
  testWidgets('renders percentage, override note and rules', (tester) async {
    await tester.pumpWidget(_wrap(StrategyTooltipCard(
      name: 'Beta users in Europe',
      kind: StrategyTooltipKind.application,
      accent: Colors.purpleAccent,
      details: StrategyTooltipDetails(
          percentage: 250000,
          percentageOverridden: true,
          attributes: [
            RolloutStrategyAttribute(
                conditional: RolloutStrategyAttributeConditional.INCLUDES,
                fieldName: 'country',
                values: ['united_kingdom', 'france'],
                type: RolloutStrategyFieldType.STRING),
            RolloutStrategyAttribute(
                conditional: RolloutStrategyAttributeConditional.EQUALS,
                fieldName: 'warehouse-id',
                values: List.generate(12, (i) => 'wh-$i'),
                type: RolloutStrategyFieldType.STRING),
          ]),
    )));

    expect(tester.takeException(), isNull);
    expect(find.text('Beta users in Europe'), findsOneWidget);
    expect(find.text('Application Strategy'), findsOneWidget);
    expect(find.text('25.0%'), findsOneWidget);
    expect(find.text('Overridden for this feature'), findsOneWidget);
    expect(find.text('united kingdom'), findsOneWidget);
    expect(find.text('+4'), findsOneWidget);
  });

  testWidgets('shows unavailable when definition is unknown', (tester) async {
    await tester.pumpWidget(_wrap(const StrategyTooltipCard(
      name: 'Shared',
      kind: StrategyTooltipKind.portfolio,
      accent: Colors.orangeAccent,
      details: null,
    )));

    expect(tester.takeException(), isNull);
    expect(find.text('Rule details unavailable'), findsOneWidget);
  });
}
