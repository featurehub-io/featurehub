import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/theme/custom_text_style.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/percentage_override.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_edit.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';

abstract class BaseRolloutStrategyCardWidget extends StatelessWidget {
  final bool editable;
  final EditingFeatureValueBloc? strBloc;
  final Widget editableHolderWidget;

  const BaseRolloutStrategyCardWidget({
    super.key,
    required this.editable,
    required this.strBloc,
    required this.editableHolderWidget,
    Object? cardColour,
  });

  Color cardColor();

  String? strategyName();

  Widget expandedSection(BuildContext context);

  int numberOfRows() {
    return 1;
  }

  List<Widget> extraColumns(BuildContext context, AppLocalizations l10n) {
    return [];
  }

  Widget serveRow(BuildContext context, AppLocalizations l10n) {
    return Row(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          Expanded(
              flex: 4,
              child: strategyName() == null
                  ? Text(l10n.strategyDefault,
                      style: Theme.of(context)
                          .textTheme
                          .bodySmall!
                          .copyWith(color: defaultTextColor))
                  : Text(strategyName()!,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.labelLarge)),
          Expanded(
              flex: 1,
              child: Text(l10n.strategyServe,
                  style: CustomTextStyle.bodySmallLight(context))),
          Expanded(flex: 4, child: editableHolderWidget),
          Expanded(flex: 3, child: expandedSection(context))
        ]);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return SizedBox(
      height: numberOfRows() * 50,
      child: InkWell(
        mouseCursor: strategyName() == null ? SystemMouseCursors.grab : null,
        child: Card(
          elevation: 0.0, // if this is not set, then colors are screwed up
          color: cardColor(),
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 2.0),
            child: numberOfRows() > 1
                ? Column(
                    children: [
                      serveRow(context, l10n),
                      ...extraColumns(context, l10n)
                    ],
                  )
                : serveRow(context, l10n),
          ),
        ),
      ),
    );
  }
}

class NullRolloutStrategyCardWidget extends BaseRolloutStrategyCardWidget {
  const NullRolloutStrategyCardWidget(
      {super.key, required super.strBloc, required super.editableHolderWidget})
      : super(editable: false);

  @override
  Color cardColor() {
    return defaultTextColor.withAlpha(38);
  }

  @override
  String? strategyName() {
    return null;
  }

  @override
  Widget expandedSection(BuildContext context) {
    return SizedBox.shrink();
  }
}

class RolloutStrategyCardWidget extends BaseRolloutStrategyCardWidget {
  final RolloutStrategy strategy;

  const RolloutStrategyCardWidget(
      {super.key,
      required super.editable,
      required super.strBloc,
      required this.strategy,
      required super.editableHolderWidget});

  @override
  Color cardColor() {
    return strategyTextColor.withAlpha(38);
  }

  @override
  Widget expandedSection(BuildContext context) {
    final bloc = strBloc!;

    return Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          EditValueStrategyLinkButton(
            editable: editable,
            rolloutStrategy: strategy,
            fvBloc: bloc,
          ),
          if (editable)
            IconButton(
              mouseCursor: SystemMouseCursors.click,
              icon: const Icon(Icons.delete, size: 16),
              onPressed: () => bloc.removeStrategy(strategy!),
            ),
        ],
      );
  }

  @override
  String? strategyName() {
    return strategy.name;
  }
}

class GroupRolloutStrategyCardWidget extends BaseRolloutStrategyCardWidget {
  final ThinGroupRolloutStrategy strategy;

  const GroupRolloutStrategyCardWidget(
      {super.key,
      required super.editable,
      required super.strBloc,
      required this.strategy,
      required super.editableHolderWidget});

  @override
  Color cardColor() {
    return groupStrategyTextColor.withAlpha(38);
  }

  @override
  Widget expandedSection(BuildContext context) {
    final bloc = strBloc!;
    final l10n = AppLocalizations.of(context)!;

    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        IconButton(
          tooltip: l10n.editStrategySettings,
          onPressed: () {
            Navigator.pop(context);
            ManagementRepositoryClientBloc.router.navigateTo(
                context, '/edit-feature-group-strategy-values',
                params: {
                  'appid': [bloc.applicationId],
                  'envid': [bloc.environmentId],
                  'groupid': [strategy.featureGroupId!]
                });
            bloc.perApplicationFeaturesBloc.mrClient
                .setCurrentEnvId(bloc.environmentId);
          },
          icon: const Icon(Icons.arrow_forward, size: 18),
        ),
      ],
    );
  }

  @override
  String? strategyName() {
    return strategy.name;
  }
}

class ApplicationRolloutStrategyCardWidget
    extends BaseRolloutStrategyCardWidget {
  final RolloutStrategyInstance strategyInstance;

  const ApplicationRolloutStrategyCardWidget(
      {super.key,
      required super.editable,
      required super.strBloc,
      required this.strategyInstance,
      required super.editableHolderWidget});

  @override
  Color cardColor() {
    return applicationStrategyTextColor.withAlpha(38);
  }

  @override
  int numberOfRows() {
    return 2;
  }

  @override
  List<Widget> extraColumns(BuildContext context, AppLocalizations l10n) {
    return [
      Row(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: <Widget>[
            Expanded(
                flex: 5,
                child: Align(
                  alignment: Alignment.centerRight,
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Text("percentage override",
                            style: Theme.of(context)
                                .textTheme
                                .bodySmall!
                                .copyWith(color: defaultTextColor)),
                  ),
                )),
            Expanded(
                flex: 1,
                child: SizedBox(
                    height: 36,
                    child: PercentageOverrideWidget(
                        onPercentageOverrideChanged: (v) => strBloc!.updateApplicationStrategyValue(),
                        strategyInstance: strategyInstance,
                        editable: editable))),
            Expanded(flex: 6, child: Text(" (optional)"))
          ])
    ];
  }

  @override
  Widget expandedSection(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        if (editable)
          IconButton(
              mouseCursor: SystemMouseCursors.click,
              icon: const Icon(Icons.edit, size: 16),
              onPressed: () {
                Navigator.pop(context);
                ManagementRepositoryClientBloc.router
                    .navigateTo(context, '/edit-application-strategy', params: {
                  'id': [strategyInstance.strategyId],
                  'appid': [strBloc!.applicationId]
                });
              }),
        if (editable)
          IconButton(
            mouseCursor: SystemMouseCursors.click,
            icon: const Icon(Icons.delete, size: 16),
            onPressed: () =>
                strBloc!.removeApplicationStrategy(strategyInstance),
          ),
      ],
    );
  }

  @override
  String? strategyName() {
    return strategyInstance.name;
  }
}

class PortfolioRolloutStrategyCardWidget extends BaseRolloutStrategyCardWidget {
  final RolloutStrategyInstance strategyInstance;

  const PortfolioRolloutStrategyCardWidget(
      {super.key,
      required super.editable,
      required super.strBloc,
      required this.strategyInstance,
      required super.editableHolderWidget});

  @override
  Color cardColor() {
    return portfolioStrategyTextColor.withAlpha(38);
  }

  @override
  Widget expandedSection(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        if (editable)
          IconButton(
              mouseCursor: SystemMouseCursors.click,
              icon: const Icon(Icons.edit, size: 16),
              onPressed: () {
                Navigator.pop(context);
                ManagementRepositoryClientBloc.router
                    .navigateTo(context, '/edit-portfolio-strategy', params: {
                  'id': [strategyInstance.strategyId],
                  'portfolioId': [strBloc!.portfolioId]
                });
              }),
        if (editable)
          IconButton(
            mouseCursor: SystemMouseCursors.click,
            icon: const Icon(Icons.delete, size: 16),
            onPressed: () => strBloc!.removePortfolioStrategy(strategyInstance),
          ),
      ],
    );
  }

  @override
  String? strategyName() {
    return strategyInstance.name;
  }
}
