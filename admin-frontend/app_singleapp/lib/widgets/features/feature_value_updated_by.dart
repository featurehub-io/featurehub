import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:timeago/timeago.dart' as timeago;

import 'feature_values_bloc.dart';

class FeatureValueUpdatedByCell extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueUpdatedByCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        builder: (context, snapshot) {
          var updatedBy = '';
          var whenUpdated = '';
          var whoUpdated = '';

          if (snapshot.hasData) {
            final fv = snapshot.data;
            if (fv.whenUpdated != null && fv.whoUpdated != null) {
              updatedBy = 'Updated by: ';
              whenUpdated = timeago.format(fv.whenUpdated.toLocal());
              whoUpdated = fv.whoUpdated.name;
            }
          }

          return Container(
              padding: EdgeInsets.only(top: 5, left: 8, right: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(updatedBy,
                      style: Theme.of(context)
                          .textTheme
                          .caption
                          .copyWith(color: Theme.of(context).buttonColor)),
                  SizedBox(height: 4.0),
                  Text(whoUpdated,
                      style: Theme.of(context).textTheme.bodyText1),
                  Text(whenUpdated, style: Theme.of(context).textTheme.caption)
                ],
              ));
        });
  }
}
