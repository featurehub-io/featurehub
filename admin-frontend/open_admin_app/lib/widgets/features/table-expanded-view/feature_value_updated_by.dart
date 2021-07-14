import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;

import '../custom_strategy_bloc.dart';

class FeatureValueUpdatedByCell extends StatelessWidget {
  final CustomStrategyBloc strBloc;

  const FeatureValueUpdatedByCell({Key? key, required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var updatedBy = '';
    var whenUpdated = '';
    var whoUpdated = '';

    if (strBloc.featureValue.whenUpdated != null &&
        strBloc.featureValue.whoUpdated != null) {
      updatedBy = 'Updated by: ';
      whenUpdated = timeago.format(strBloc.featureValue.whenUpdated!.toLocal());
      whoUpdated = strBloc.featureValue.whoUpdated!.name!;
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
                        .caption!
                        .copyWith(color: Theme.of(context).buttonColor)),
                Text(whoUpdated, style: Theme.of(context).textTheme.bodyText1),
              ],
            ),
            Text(whenUpdated, style: Theme.of(context).textTheme.caption)
          ],
        ));
  }
}
