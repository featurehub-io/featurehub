import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:timeago/timeago.dart' as timeago;

import 'feature_values_bloc.dart';

class FeatureValueUpdatedByCell extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  FeatureValueUpdatedByCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    var updatedBy = '';
    var whenUpdated = '';
    var whoUpdated = '';

    if (featureValue.whenUpdated != null && featureValue.whoUpdated != null) {
      updatedBy = 'Updated by: ';
      whenUpdated = timeago.format(featureValue.whenUpdated.toLocal());
      whoUpdated = featureValue.whoUpdated.name;
    }

    return Container(
        padding: EdgeInsets.only(top: 16.0, left: 8, right: 8),
        child: Column(
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(updatedBy,
                    style: Theme.of(context)
                        .textTheme
                        .caption
                        .copyWith(color: Theme.of(context).buttonColor)),
                Text(whoUpdated, style: Theme.of(context).textTheme.bodyText1),
              ],
            ),
            Text(whenUpdated, style: Theme.of(context).textTheme.caption)
          ],
        ));
  }
}
