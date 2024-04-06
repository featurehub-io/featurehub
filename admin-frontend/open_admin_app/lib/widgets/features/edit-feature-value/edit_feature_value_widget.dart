import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/theme/custom_text_style.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_info_card.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/retire_feature_value_checkbox_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_add.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/strategy_card.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class EditFeatureValueWidget extends StatefulWidget {
  final EditingFeatureValueBloc bloc;

  const EditFeatureValueWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  State<EditFeatureValueWidget> createState() => _EditFeatureValueWidgetState();
}

class _EditFeatureValueWidgetState extends State<EditFeatureValueWidget> {
  @override
  Widget build(BuildContext context) {
    final roles = widget.bloc.environmentFeatureValue.roles;
    final canSave = roles.contains(RoleType.CHANGE_VALUE) ||
        roles.contains(RoleType.LOCK) ||
        roles.contains(RoleType.UNLOCK);

    return StreamBuilder<List<RolloutStrategy>>(
        stream: widget.bloc.strategies,
        builder: (streamCtx, strategiesLatest) {
          return Column(
            children: [
              StreamBuilder<bool>(
                  stream: widget.bloc.isFeatureValueUpdatedStream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData && snapshot.data! && canSave) {
                      return SizedBox(
                        height: 46.0,
                        child: Container(
                          color:
                              Theme.of(context).snackBarTheme.backgroundColor,
                          child: Row(children: [
                            const SizedBox(
                              width: 8.0,
                            ),
                            const Text('You have unsaved changes, save?'),
                            TextButton(
                                style: TextButton.styleFrom(
                                  foregroundColor: Theme.of(context)
                                      .snackBarTheme
                                      .actionTextColor,
                                ),
                                onPressed: () {
                                  Navigator.pop(context); //close the side panel
                                },
                                child: const Text("Cancel")),
                            FilledButton(
                                onPressed: () async {
                                  try {
                                    await widget.bloc.saveFeatureValueUpdates();
                                    Navigator.pop(
                                        context); //close the side panel
                                    widget.bloc.perApplicationFeaturesBloc
                                        .mrClient
                                        .addSnackbar(Text(
                                            'Feature ${widget.bloc.feature.name.toUpperCase()} '
                                            'in the environment ${widget.bloc.environmentFeatureValue.environmentName.toUpperCase()} has been updated!'));
                                  } catch (e, s) {
                                    widget.bloc.perApplicationFeaturesBloc
                                        .mrClient
                                        .dialogError(e, s);
                                  }
                                },
                                child: const Text("Save")),
                          ]),
                        ),
                      );
                    } else {
                      return const SizedBox.shrink();
                    }
                  }),
              Expanded(
                child: SingleChildScrollView(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              widget.bloc.feature.name,
                              style: CustomTextStyle.bodyMediumBold(context),
                            ),
                            IconButton(
                              icon: const Icon(Icons.close, size: 20),
                              onPressed: () => Navigator.of(context).pop(),
                            ),
                          ],
                        ),
                      ),
                      const FHPageDivider(),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              "Environment",
                              style: CustomTextStyle.bodySmallLight(context),
                            ),
                            Text(widget
                                .bloc.environmentFeatureValue.environmentName),
                          ],
                        ),
                      ),
                      Card(
                        elevation: 3.0,
                        shadowColor: Colors.transparent,
                        child: Padding(
                          padding: const EdgeInsets.only(
                              top: 8.0, bottom: 16.0, left: 8.0, right: 8.0),
                          child: Column(
                            children: [
                              const SizedBox(height: 16.0),
                              Row(
                                children: [
                                  Text(
                                    "Locked status",
                                    style:
                                        CustomTextStyle.bodySmallLight(context),
                                  ),
                                  const SizedBox(
                                    width: 4.0,
                                  ),
                                  const FHInfoCardWidget(
                                      message:
                                          "Locking mechanism provides an additional safety net for feature changes when deploying incomplete code to production."
                                          " Locked status prevents any changes to default value, "
                                          "strategies, strategy values and 'retired' status. "
                                          "Typically, developers keep features locked "
                                          "to indicate they are not ready to be turned on for testers, product owners, customers and other stakeholders."),
                                ],
                              ),
                              const SizedBox(height: 4.0),
                              LockUnlockSwitch(
                                environmentFeatureValue:
                                    widget.bloc.environmentFeatureValue,
                                fvBloc: widget.bloc,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16.0),
                      StreamBuilder<FeatureValue>(
                          stream: widget.bloc.currentFv,
                          builder: (context, featureValueLatest) {
                            if (featureValueLatest.hasData) {
                              final canChangeValue = widget
                                  .bloc.environmentFeatureValue.roles
                                  .contains(RoleType.CHANGE_VALUE);
                              var editable = !featureValueLatest.data!.locked &&
                                  canChangeValue;
                              List<Widget> widgets = [];
                              if (strategiesLatest.hasData) {
                                widgets = strategiesLatest.data!
                                    .map((RolloutStrategy strategy) {
                                  return Column(
                                    children: [
                                      StrategyCard(
                                          key: ValueKey(strategy),
                                          strBloc: widget.bloc,
                                          rolloutStrategy: strategy,
                                          featureValueType:
                                              widget.bloc.feature.valueType),
                                    ],
                                  );
                                }).toList();
                              }
                              return Card(
                                elevation: 3.0,
                                shadowColor: Colors.transparent,
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 8.0, vertical: 24.0),
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text("Default value",
                                          style: CustomTextStyle.bodySmallLight(
                                              context)),
                                      StrategyCard(
                                          strBloc: widget.bloc,
                                          featureValueType:
                                              widget.bloc.feature.valueType),
                                      const SizedBox(height: 16.0),
                                      Row(
                                        children: [
                                          Text("Strategy variations",
                                              style: CustomTextStyle
                                                  .bodySmallLight(context)),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "Add a strategy variation to serve a value other than default. "
                                                  "You can change strategies evaluation order by dragging and dropping the cards below. "
                                                  "Strategies are evaluated in order from top to bottom. Evaluation stops when it hits a matching strategy."
                                                  " 'Group Strategy' evaluation comes last. If no strategies match, then 'default' feature value is served."),
                                          const SizedBox(
                                            width: 8.0,
                                          ),
                                          if (editable)
                                            AddStrategyButton(
                                              bloc: widget.bloc,
                                            ),
                                        ],
                                      ),
                                      if (widgets.isEmpty)
                                        const Text("No strategies"),
                                      buildReorderableListView(
                                          widgets,
                                          featureValueLatest,
                                          canChangeValue,
                                          strategiesLatest,
                                          widget.bloc),
                                      const SizedBox(height: 24.0),
                                      Row(
                                        children: [
                                          Text("Group strategy variations",
                                              style: CustomTextStyle
                                                  .bodySmallLight(context)),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "Feature groups are recommended when you want to set the same strategy for multiple features. "
                                                  "Feature group strategy can be created and edited from the Feature Groups screen.")
                                        ],
                                      ),
                                      if (featureValueLatest
                                              .data
                                              ?.featureGroupStrategies
                                              ?.isEmpty ==
                                          true)
                                        const Text("No group strategies"),
                                      if (featureValueLatest
                                              .data
                                              ?.featureGroupStrategies
                                              ?.isNotEmpty ==
                                          true)
                                        for (var groupStrategy
                                            in featureValueLatest
                                                .data!.featureGroupStrategies!)
                                          StrategyCard(
                                              groupRolloutStrategy:
                                                  groupStrategy,
                                              strBloc: widget.bloc,
                                              featureValueType: widget
                                                  .bloc.feature.valueType),
                                      const SizedBox(height: 24.0),
                                      Row(
                                        children: [
                                          Text(
                                            "Retired status",
                                            style:
                                                CustomTextStyle.bodySmallLight(
                                                    context),
                                          ),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "When feature flag is not needed any longer in your application,"
                                                  " and ready to be removed, you can first 'retire' this feature in a given environment"
                                                  " to test how your application behaves. This means that the feature won't be visible by the SDKs,"
                                                  " imitating the 'deleted' state. You can uncheck the box to 'un-retire' a feature if you change your mind"
                                                  " as this operation is reversible. Once you retire feature values across all the environments"
                                                  "  and test that your application behaves as expected, you can delete your entire feature.")
                                        ],
                                      ),
                                      RetireFeatureValueCheckboxWidget(
                                          environmentFeatureValue: widget
                                              .bloc.environmentFeatureValue,
                                          fvBloc: widget.bloc,
                                          editable: editable,
                                          retired: widget.bloc
                                              .currentFeatureValue.retired),
                                      //this is where we need to pass retired from the actual value
                                      const SizedBox(height: 16.0),
                                      FeatureValueUpdatedByCell(
                                        strBloc: widget.bloc,
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            } else {
                              return const SizedBox.shrink();
                            }
                          }),
                    ])),
              ),
            ],
          );
        });
  }

  ReorderableListView buildReorderableListView(
      List<Widget> widgets,
      AsyncSnapshot<FeatureValue> featureValueLatest,
      bool canChangeValue,
      AsyncSnapshot<List<RolloutStrategy>> strategiesLatest,
      EditingFeatureValueBloc bloc) {
    return ReorderableListView(
      shrinkWrap: true,
      buildDefaultDragHandles: false,
      children: <Widget>[
        for (Widget wid in widgets)
          ReorderableDragStartListener(
              key: ValueKey(wid),
              enabled: !featureValueLatest.data!.locked && canChangeValue,
              index: widgets.indexOf(wid),
              child: wid)
      ],
      onReorder: (int oldIndex, int newIndex) {
        if (newIndex > oldIndex) {
          newIndex -= 1;
        }
        final items = strategiesLatest.data![oldIndex];

        strategiesLatest.data!
          ..removeWhere((element) => element.id == items.id)
          ..insert(newIndex, items);

        widget.bloc.updateStrategyValue();
      },
    );
  }
}
