import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_generic.dart';
import 'feature_values_bloc.dart';

class FeatureValueEditLockedCell extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  FeatureValueEditLockedCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          // must always return the same "shaped" data in a table cell
          final disabled =
              (!environmentFeatureValue.roles.contains(RoleType.UNLOCK) &&
                      snap.data?.locked == true) ||
                  (snap.data?.locked == false &&
                      !environmentFeatureValue.roles.contains(RoleType.LOCK));

          final locked =
              snap.data == null ? false : (snap.data.locked ?? false);

          return Container(
            height: 60.0,
            child: Row(
              children: <Widget>[
                locked
                    ? Icon(
                        Icons.lock_outline,
                        color: Colors.black54,
                      )
                    : Icon(Icons.lock_open, color: Colors.black54),
                Checkbox(
                  activeColor: Theme.of(context).primaryColor,
                  value: locked,
                  onChanged: disabled
                      ? null
                      : (value) {
                          snap.data.locked = value;
                          fvBloc.updatedFeature(
                              environmentFeatureValue.environmentId);
                        },
                )
              ],
            ),
          );
        });
  }
}

class FeatureValueEditLocked {
  static TableRow build(BuildContext context, LineStatusFeature featureStatuses,
      Feature feature) {
    final fvBloc = BlocProvider.of<FeatureValuesBloc>(context);
    final bs = BorderSide(
        color: Theme.of(context).dividerColor,
        width: 1.0,
        style: BorderStyle.solid);
    return TableRow(
        decoration: BoxDecoration(
          border: Border(top: bs),
        ),
        children: [
          FeatureValueNameCell(
            feature: feature,
          ),
          ...featureStatuses.environmentFeatureValues
              .map((e) => FeatureValueEditLockedCell(
                  environmentFeatureValue: e, feature: feature, fvBloc: fvBloc))
              .toList()
        ]);
  }
}
