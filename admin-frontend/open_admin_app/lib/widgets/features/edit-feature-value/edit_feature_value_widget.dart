import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
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
          return Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  Expanded(
                    child: SingleChildScrollView(
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                          Text(
                            widget.bloc.feature.name,
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                          const SizedBox(height: 8.0),
                          Text(widget
                              .bloc.environmentFeatureValue.environmentName),
                          const SizedBox(height: 16.0),
                          LockUnlockSwitch(
                            environmentFeatureValue:
                                widget.bloc.environmentFeatureValue,
                            fvBloc: widget.bloc,
                          ),
                          StreamBuilder<FeatureValue>(
                              stream: widget.bloc.currentFv,
                              builder: (context, featureValueLatest) {
                                if (featureValueLatest.hasData) {
                                  final canChangeValue = widget
                                      .bloc.environmentFeatureValue.roles
                                      .contains(RoleType.CHANGE_VALUE);
                                  var editable =
                                      !featureValueLatest.data!.locked &&
                                          canChangeValue;
                                  List<Widget> widgets = [];
                                  if (strategiesLatest.hasData) {
                                    widgets = strategiesLatest.data!
                                        .map((RolloutStrategy strategy) {
                                      return StrategyCard(
                                          key: ValueKey(strategy),
                                          strBloc: widget.bloc,
                                          rolloutStrategy: strategy,
                                          featureValueType:
                                              widget.bloc.feature.valueType);
                                    }).toList();
                                  }
                                  return Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      StrategyCard(
                                          strBloc: widget.bloc,
                                          featureValueType:
                                              widget.bloc.feature.valueType),
                                      if (strategiesLatest.hasData)
                                        buildReorderableListView(
                                            widgets,
                                            featureValueLatest,
                                            canChangeValue,
                                            strategiesLatest,
                                            widget.bloc),
                                      const SizedBox(height: 16.0),
                                      if (featureValueLatest.data!
                                                  .featureGroupStrategies !=
                                              null &&
                                          featureValueLatest
                                              .data!
                                              .featureGroupStrategies!
                                              .isNotEmpty)
                                        Padding(
                                          padding:
                                              const EdgeInsets.only(left: 12.0),
                                          child: Text("Group Strategy",
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .labelSmall),
                                        ),
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
                                      const SizedBox(height: 8.0),
                                      if (editable)
                                        AddStrategyButton(
                                          bloc: widget.bloc,
                                        ),
                                      const SizedBox(height: 16.0),
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
                                      const SizedBox(height: 24.0),
                                      ButtonBar(
                                          alignment: MainAxisAlignment.end,
                                          children: [
                                            TextButton(
                                                onPressed: () {
                                                  Navigator.pop(
                                                      context); //close the side panel
                                                },
                                                child: const Text("Cancel")),
                                            FilledButton(
                                                onPressed: canSave
                                                    ? () async {
                                                        try {
                                                          await widget.bloc
                                                              .saveFeatureValueUpdates();
                                                          Navigator.pop(
                                                              context); //close the side panel
                                                          widget
                                                              .bloc
                                                              .perApplicationFeaturesBloc
                                                              .mrClient
                                                              .addSnackbar(Text(
                                                                  'Feature ${widget.bloc.feature.name.toUpperCase()} '
                                                                  'in the environment ${widget.bloc.environmentFeatureValue.environmentName.toUpperCase()} has been updated!'));
                                                        } catch (e, s) {
                                                          widget
                                                              .bloc
                                                              .perApplicationFeaturesBloc
                                                              .mrClient
                                                              .dialogError(
                                                                  e, s);
                                                        }
                                                      }
                                                    : null,
                                                child: const Text("Save")),
                                          ])
                                    ],
                                  );
                                } else {
                                  return const SizedBox.shrink();
                                }
                              }),
                        ])),
                  ),
                ],
              ));
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

        widget.bloc.updateStrategyAndFeatureValue();
      },
    );
  }
}
