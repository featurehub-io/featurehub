import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/theme/custom_text_style.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_edit.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';

class StrategyCardWidget extends StatelessWidget {
  final bool editable;
  final Widget editableHolderWidget;
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  const StrategyCardWidget(
      {super.key,
      required this.editable,
      required this.editableHolderWidget,
      this.rolloutStrategy,
      required this.strBloc,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return SizedBox(
      height: 50,
      child: InkWell(
        mouseCursor:
            rolloutStrategy != null || applicationRolloutStrategy != null
                ? SystemMouseCursors.grab
                : null,
        child: Card(
          elevation: 0.0, // if this is not set, then colors are screwed up
          color: rolloutStrategy != null
              ? strategyTextColor.withAlpha(38)
              : groupRolloutStrategy != null
                  ? groupStrategyTextColor.withAlpha(38)
                  : applicationRolloutStrategy != null
                      ? applicationStrategyTextColor.withAlpha(38)
                      : defaultTextColor.withAlpha(38),
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 2.0),
            child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Expanded(
                      flex: 4,
                      child: groupRolloutStrategy != null
                          ? Text(groupRolloutStrategy!.name,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.labelLarge)
                          : applicationRolloutStrategy != null
                              ? Text(applicationRolloutStrategy!.name!,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: Theme.of(context).textTheme.labelLarge)
                              : (rolloutStrategy?.name == null
                                  ? Text(l10n.strategyDefault,
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodySmall!
                                          .copyWith(color: defaultTextColor))
                                  : Text(rolloutStrategy!.name,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: Theme.of(context)
                                          .textTheme
                                          .labelLarge))),
                  Expanded(
                      flex: 1,
                      child: Text(l10n.strategyServe,
                          style: CustomTextStyle.bodySmallLight(context))),
                  Expanded(flex: 4, child: editableHolderWidget),
                  // UI mockup only: "set percentage <value> %"
                  const Expanded(flex: 4, child: SetPercentageField()),
                  if (rolloutStrategy != null && groupRolloutStrategy == null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          EditValueStrategyLinkButton(
                            editable: editable,
                            rolloutStrategy: rolloutStrategy!,
                            fvBloc: strBloc,
                          ),
                          if (editable)
                            IconButton(
                              mouseCursor: SystemMouseCursors.click,
                              icon: const Icon(Icons.delete, size: 16),
                              onPressed: () =>
                                  strBloc.removeStrategy(rolloutStrategy!),
                            ),
                        ],
                      ),
                    ),
                  if (groupRolloutStrategy != null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          IconButton(
                            tooltip: l10n.editStrategySettings,
                            onPressed: () {
                              Navigator.pop(context);
                              ManagementRepositoryClientBloc.router.navigateTo(
                                  context,
                                  '/edit-feature-group-strategy-values',
                                  params: {
                                    'appid': [strBloc.applicationId],
                                    'envid': [strBloc.environmentId],
                                    'groupid': [
                                      groupRolloutStrategy!.featureGroupId!
                                    ]
                                  });
                              strBloc.perApplicationFeaturesBloc.mrClient
                                  .setCurrentEnvId(strBloc.environmentId);
                            },
                            icon: const Icon(Icons.arrow_forward, size: 18),
                          ),
                        ],
                      ),
                    ),
                  if (applicationRolloutStrategy != null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          if (editable)
                            IconButton(
                                mouseCursor: SystemMouseCursors.click,
                                icon: const Icon(Icons.edit, size: 16),
                                onPressed: () {
                                  Navigator.pop(context);
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(
                                          context, '/edit-application-strategy',
                                          params: {
                                        'id': [
                                          applicationRolloutStrategy!.strategyId
                                        ],
                                        'appid': [strBloc.applicationId]
                                      });
                                }),
                          if (editable)
                            IconButton(
                              mouseCursor: SystemMouseCursors.click,
                              icon: const Icon(Icons.delete, size: 16),
                              onPressed: () =>
                                  strBloc.removeApplicationStrategy(
                                      applicationRolloutStrategy!),
                            ),
                        ],
                      ),
                    ),
                ]),
          ),
        ),
      ),
    );
  }
}

/// UI mockup only — renders "set percentage [ value ] %" with an input that
/// accepts a number between 0 and 100. No backend wiring yet.
class SetPercentageField extends StatefulWidget {
  const SetPercentageField({super.key});

  @override
  State<SetPercentageField> createState() => _SetPercentageFieldState();
}

class _SetPercentageFieldState extends State<SetPercentageField> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text('rollout at',
            style: CustomTextStyle.bodySmallLight(context)),
        const SizedBox(width: 6.0),
        SizedBox(
          width: 48.0,
          child: TextField(
            controller: _controller,
            keyboardType: TextInputType.number,
            textAlign: TextAlign.center,
            decoration: const InputDecoration(
              isDense: true,
              contentPadding:
                  EdgeInsets.symmetric(horizontal: 6.0, vertical: 8.0),
              border: OutlineInputBorder(),
            ),
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              // Clamp to 0-100
              TextInputFormatter.withFunction((oldValue, newValue) {
                if (newValue.text.isEmpty) return newValue;
                final value = int.tryParse(newValue.text);
                if (value == null || value < 0 || value > 100) {
                  return oldValue;
                }
                return newValue;
              }),
            ],
          ),
        ),
        const SizedBox(width: 4.0),
        Text('%', style: CustomTextStyle.bodySmallLight(context)),
      ],
    );
  }
}
