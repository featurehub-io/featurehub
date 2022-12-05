import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/experiment_data_table.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_blocv2.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/retire_feature_value_checkbox_widget.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/split_rollout_button.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_card.dart';

// represents the editing of the states of a single boolean flag on a single environment

class EditFeatureValueWidget extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final FeatureValueType featureValueType;
  final PerApplicationFeaturesBloc perApplicationFeaturesBloc;
  final Feature feature;
  final ApplicationFeatureValues afv;
  final FeatureValue fv;
  final FeaturesDataSource featuresDataSource;

  const EditFeatureValueWidget(

      {Key? key,
        required this.fv,
      required this.environmentFeatureValue,
      required this.featureValueType,
      required this.perApplicationFeaturesBloc,
      required this.feature,
      required this.afv, required this.featuresDataSource,
        })
      : super(key: key);

  @override
  State<EditFeatureValueWidget> createState() => _EditFeatureValueWidgetState();
}

class _EditFeatureValueWidgetState extends State<EditFeatureValueWidget> {


  @override
  Widget build(BuildContext context) {
    var fvBloc = PerFeatureStateTrackingBlocV2(
        widget.afv.applicationId,
        widget.feature,
        widget.perApplicationFeaturesBloc.mrClient,
        widget.fv,
        widget.perApplicationFeaturesBloc,
        widget.afv);
    final strategyBloc =
        fvBloc.matchingCustomStrategyBloc(widget.environmentFeatureValue);

    return StreamBuilder<List<RolloutStrategy>>(
        stream: strategyBloc.strategies,
        builder: (streamCtx, snap) {
          return Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Feature: ${widget.feature.name}', style: Theme.of(context).textTheme.titleLarge,),
                Text('Environment: ${widget.environmentFeatureValue.environmentName}', style: Theme.of(context).textTheme.titleLarge),
                LockUnlockSwitch(
                  environmentFeatureValue: widget.environmentFeatureValue,
                  fvBloc: fvBloc,
                ),
                StreamBuilder<List<RolloutStrategy>>(
                    stream: strategyBloc.strategies,
                    builder: (context, snapshot) {
                      return Column(
                        children: [
                          StrategyCard(
                              strBloc: strategyBloc,
                              featureValueType: widget.featureValueType),
                          if (snapshot.hasData)
                            for (RolloutStrategy strategy in snapshot.data!)
                              StrategyCard(
                                  strBloc: strategyBloc,
                                  rolloutStrategy: strategy,
                                  featureValueType: widget.featureValueType),
                        ],
                      );
                    }),
                StreamBuilder<FeatureValue>(
                    stream: fvBloc.currentFv,
                    builder: (context, snapshot) {
                      if (snapshot.hasData) {
                        final canChangeValue = widget.environmentFeatureValue.roles
                            .contains(RoleType.CHANGE_VALUE);
                        var editable = !snapshot.data!.locked && canChangeValue;
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: 16.0),
                            AddStrategyButton(
                                bloc: strategyBloc, editable: editable),
                            const SizedBox(height: 16.0),
                            RetireFeatureValueCheckboxWidget(
                                environmentFeatureValue: widget.environmentFeatureValue,
                                fvBloc: fvBloc,
                                editable: editable,
                                retired: fvBloc.currentFeatureValue!.retired ?? false),
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
                Align(
                  alignment: Alignment.bottomCenter,
                  child: OutlinedButton(onPressed: () {
                    fvBloc.saveFeatureValueUpdates();
                    Navigator.pop(context); //close the side panel
                    widget.featuresDataSource.buildDataGridRows();
                    widget.featuresDataSource.updateDataGridSource();
                  }, child: const Text("Save")),
                )
              ],
            ),
          );
        });
  }
}
