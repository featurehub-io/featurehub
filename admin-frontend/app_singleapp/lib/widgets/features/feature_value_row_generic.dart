import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_copy_to_clipboard.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/features/create-update-feature-dialog-widget.dart';
import 'package:app_singleapp/widgets/features/delete_feature_widget.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:timeago/timeago.dart' as timeago;

import 'feature_values_bloc.dart';

/// show the key name
class FeatureValueNameCell extends StatelessWidget {
  final Feature feature;

  const FeatureValueNameCell({Key key, @required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 2.0, top: 16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text('Feature key',
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context)
                  .textTheme
                  .caption
                  .copyWith(color: Theme.of(context).buttonColor)),
          SizedBox(height: 2.0),
          Text(feature.key,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontFamily: 'source', fontSize: 12)),
          FHCopyToClipboard(
            tooltipMessage: 'Copy key',
            copyString: feature.key,
          ),
          SizedBox(
            height: 12.0,
          ),
          if (feature.link != null && feature.link.isNotEmpty)
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                Text('Reference link',
                    style: Theme.of(context)
                        .textTheme
                        .caption
                        .copyWith(color: Theme.of(context).buttonColor)),
                FHCopyToClipboard(
                  tooltipMessage: '${feature.link}',
                  copyString: feature.link,
                )
              ],
            ),
//Comment out Alias key until we implement proper analytics
//          feature.alias != null && feature.alias.isNotEmpty
//              ? Column(
//                  crossAxisAlignment: CrossAxisAlignment.start,
//                  children: <Widget>[
//                    Text('Feature alias key',
//                        style: Theme.of(context)
//                            .textTheme
//                            .caption
//                            .copyWith(color: Theme.of(context).buttonColor)),
//                    SizedBox(height: 2.0),
//                    Row(
//                      children: <Widget>[
//                        Text(feature.alias,
//                            style:
//                                TextStyle(fontFamily: 'source', fontSize: 12)),
//                        FHCopyToClipboard(
//                          tooltipMessage: 'Copy alias key',
//                          copyString: feature.alias,
//                        )
//                      ],
//                    )
//                  ],
//                )
//              : Container(),
        ],
      ),
    );
  }
}

/// represents the edit/delete icons that sit underneath a feature
class FeatureEditDeleteCell extends StatelessWidget {
  final Feature feature;

  const FeatureEditDeleteCell({Key key, @required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    return FutureBuilder<bool>(
        future: bloc.mrClient.personState
            .personCanEditFeaturesForCurrentApplication(bloc.applicationId),
        builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
          return Padding(
              padding: const EdgeInsets.only(top: 16.0),
              child: Wrap(spacing: 0.0, runSpacing: 0.0, children: [
                snapshot.hasData && snapshot.data
                    ? FHIconButton(
                        icon: Icon(
                          Icons.edit,
                          color: Theme.of(context).buttonColor,
                        ),
                        onPressed: () => bloc.mrClient.addOverlay(
                            (BuildContext context) => CreateFeatureDialogWidget(
                                bloc: bloc, feature: feature)))
                    : FHIconButton(
                        icon: Icon(
                          Icons.edit,
                          color: Theme.of(context).disabledColor,
                        ),
                        onPressed: () => null),
                snapshot.hasData && snapshot.data
                    ? FHIconButton(
                        icon: Icon(
                          Icons.delete,
                          color: Theme.of(context).buttonColor,
                        ),
                        onPressed: () =>
                            bloc.mrClient.addOverlay((BuildContext context) {
                              return FeatureDeleteDialogWidget(
                                feature: feature,
                                bloc: bloc,
                              );
                            }))
                    : FHIconButton(
                        icon: Icon(
                          Icons.delete,
                          color: Theme.of(context).disabledColor,
                        ),
                        onPressed: () => null,
                      )
              ]));
        });
  }
}

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
              padding: EdgeInsets.only(top: 5),
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

class FeatureValueActionCell extends StatelessWidget {
  final Function closeCallback;

  const FeatureValueActionCell({Key key, this.closeCallback}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final fvBloc = BlocProvider.of<FeatureValuesBloc>(context);

    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Row(
        children: <Widget>[
          FHFlatButtonTransparent(
            title: 'Reset',
            onPressed: () async {
              fvBloc.reset();
            },
          ),
          FHFlatButton(
            title: 'Save',
            onPressed: () async {
              // delegate _everything_ to the bloc, don't fiddle with bloc internals here on pain of being hunted down
              // notify the FeatureValueBloc to persist and on success, notify the FeatureStatusBloc to refresh this line
              // do not automatically hide the bloc
              final success = await fvBloc.updateDirtyStates();
              if (success) {
                closeCallback();
              }
            },
          )
        ],
      ),
    );
  }
}
