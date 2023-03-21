import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/retire_feature_value_checkbox_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_rollout_button.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/strategy_card.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class EditFeatureValueWidget extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerApplicationFeaturesBloc perApplicationFeaturesBloc;
  final Feature feature;
  final FeatureValue fv;

  const EditFeatureValueWidget({
    Key? key,
    required this.fv,
    required this.environmentFeatureValue,
    required this.perApplicationFeaturesBloc,
    required this.feature,
  }) : super(key: key);

  @override
  State<EditFeatureValueWidget> createState() => _EditFeatureValueWidgetState();
}

class _EditFeatureValueWidgetState extends State<EditFeatureValueWidget> {
  @override
  Widget build(BuildContext context) {
    var fvBloc = widget.perApplicationFeaturesBloc.perFeatureStateTrackingBloc(
      widget.feature,
      widget.fv,
    );
    final strategyBloc =
        fvBloc.matchingCustomStrategyBloc(widget.environmentFeatureValue);

    return Container(
      color: Theme.of(context).canvasColor,
      child: StreamBuilder<List<RolloutStrategy>>(
          stream: strategyBloc.strategies,
          builder: (streamCtx, snap) {
            return Padding(
              padding: const EdgeInsets.all(16.0),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.feature.name,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 8.0),
                    Text('${widget.environmentFeatureValue.environmentName}'),
                    const SizedBox(height: 16.0),
                    LockUnlockSwitch(
                      environmentFeatureValue: widget.environmentFeatureValue,
                      fvBloc: fvBloc,
                    ),
                    StreamBuilder<List<RolloutStrategy>>(
                        stream: strategyBloc.strategies,
                        builder: (context, snapshot) {
                          List<Widget> _widgets = [];
                          if (snapshot.hasData) {
                            _widgets =
                                snapshot.data!.map((RolloutStrategy strategy) {
                              return StrategyCard(
                                  key: ValueKey(strategy),
                                  strBloc: strategyBloc,
                                  rolloutStrategy: strategy,
                                  featureValueType: widget.feature.valueType!);
                            }).toList();
                          }
                          return Column(
                            children: [
                              StrategyCard(
                                  strBloc: strategyBloc,
                                  featureValueType: widget.feature.valueType!),
                              if (snapshot.hasData)
                                SizedBox(
                                  height: 300,
                                  child: ReorderableListView(
                                    buildDefaultDragHandles: false,
                                    children: <Widget>[
                                      for (Widget wid in _widgets)
                                        ReorderableDragStartListener(
                                            key: ValueKey(wid),
                                            child: wid,
                                            index: _widgets.indexOf(wid))
                                    ],
                                    onReorder: (int oldIndex, int newIndex) {
                                      if (newIndex > oldIndex) {
                                        newIndex -= 1;
                                      }

// First get the items at the old index
                                      final items = snapshot.data![oldIndex];

//Then remove it from the old position and insert it at the new one
//                                       List<RolloutStrategy> reorderedList =
//                                           List.from(snapshot.data!)
//                                             ..removeWhere((element) =>
//                                                 element.id == items.id)
//                                             ..insert(newIndex, items);

                                      (snapshot.data!)
                                        ..removeWhere((element) =>
                                        element.id == items.id)
                                        ..insert(newIndex, items);

                                      strategyBloc
                                          .updateStrategyAndFeatureValue();
                                    },
                                  ),
                                )
                            ],
                          );
                        }),
                    StreamBuilder<FeatureValue>(
                        stream: fvBloc.currentFv,
                        builder: (context, snapshot) {
                          if (snapshot.hasData) {
                            final canChangeValue = widget
                                .environmentFeatureValue.roles
                                .contains(RoleType.CHANGE_VALUE);
                            var editable =
                                !snapshot.data!.locked && canChangeValue;
                            return Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const SizedBox(height: 16.0),
                                AddStrategyButton(
                                    bloc: strategyBloc, editable: editable),
                                const SizedBox(height: 16.0),
                                RetireFeatureValueCheckboxWidget(
                                    environmentFeatureValue:
                                        widget.environmentFeatureValue,
                                    fvBloc: fvBloc,
                                    editable: editable,
                                    retired:
                                        fvBloc.currentFeatureValue!.retired ??
                                            false),
                                //this is where we need to pass retired from the actual value
                              ],
                            );
                          } else {
                            return Container();
                          }
                        }),
                    const SizedBox(height: 16.0),
                    FeatureValueUpdatedByCell(
                      strBloc: strategyBloc,
                    ),
                    const SizedBox(height: 24.0),
                    ButtonBar(alignment: MainAxisAlignment.center, children: [
                      TextButton(
                          onPressed: () {
                            Navigator.pop(context); //close the side panel
                          },
                          child: const Text("Cancel")),
                      ElevatedButton(
                          onPressed: () async {
                            try {
                              await fvBloc.saveFeatureValueUpdates();
                              Navigator.pop(context); //close the side panel
                              widget.perApplicationFeaturesBloc.mrClient
                                  .addSnackbar(Text(
                                      'Feature ${widget.feature.name.toUpperCase()} '
                                      'in the environment ${widget.environmentFeatureValue.environmentName?.toUpperCase()} has been updated!'));
                            } catch (e, s) {
                              widget.perApplicationFeaturesBloc.mrClient
                                  .dialogError(e, s);
                            }
                          },
                          child: const Text("Save")),
                    ])
                  ],
                ),
              ),
            );
          }),
    );
  }
}
