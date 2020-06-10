import 'package:app_singleapp/widgets/common/fh_link.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:timeago/timeago.dart' as timeago;

import 'feature_value_status_tags.dart';
import 'features_latest_bloc.dart';

class LatestFeaturesWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    FeaturesLatestBloc bloc = BlocProvider.of(context);
    final BorderSide bs = BorderSide(color: Theme.of(context).dividerColor);

    return StreamBuilder<EnvironmentFeaturesResult>(
        stream: bloc.featuresListStream,
        builder: (context, snapshot) {
          if (snapshot.hasData && snapshot.data.featureValues.isNotEmpty) {
            return Column(
              children: <Widget>[
                Container(
                    decoration: BoxDecoration(
                      border: Border(bottom: bs, left: bs, right: bs, top: bs),
                      color: Theme.of(context).cardColor,
                    ),
                    padding: EdgeInsets.only(top: 30),
                    child: Table(
                      defaultVerticalAlignment:
                          TableCellVerticalAlignment.middle,
                      children: _tableRows(snapshot.data, context, bloc),
                    )),
              ],
            );
          }
          if (!snapshot.hasData) {
            return Text('Loading...');
          } else {
            return Text('There are no recent features to display');
          }
        });
  }

  List<TableRow> _tableRows(EnvironmentFeaturesResult featuresList,
      BuildContext context, FeaturesLatestBloc bloc) {
    List<TableRow> _rows;
    _rows = [];
    _rows.add(TableRow(
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(color: Theme.of(context).dividerColor),
          ),
        ),
        children: <Widget>[
          Container(
            padding: EdgeInsets.fromLTRB(15, 15, 0, 15),
            child:
                Text('Feature', style: Theme.of(context).textTheme.subtitle2),
          ),
          Text('Value', style: Theme.of(context).textTheme.subtitle2),
          Text('Application', style: Theme.of(context).textTheme.subtitle2),
          Text('Environment', style: Theme.of(context).textTheme.subtitle2),
          Text('Last modified', style: Theme.of(context).textTheme.subtitle2),
          Text('Audit log', style: Theme.of(context).textTheme.subtitle2),
        ]));

    featuresList.featureValues.forEach((fv) =>
        _rows.add(_buildFeatureValueRow(context, fv, featuresList, bloc)));
    return _rows;
  }

  TableRow _buildFeatureValueRow(BuildContext context, FeatureValue fv,
      EnvironmentFeaturesResult featuresList, FeaturesLatestBloc bloc) {
    final findFeature = _findFeature(fv, featuresList);
    return TableRow(
        decoration: BoxDecoration(
            border: Border(
                bottom: BorderSide(color: Theme.of(context).dividerColor))),
        children: <Widget>[
          Container(
              padding: EdgeInsets.fromLTRB(15, 10, 0, 10),
              child: findFeature == null
                  ? SizedBox.shrink()
                  : FHLinkWidget(
                      href:
                          '/feature-values?applicationId=${_findApplication(fv, featuresList).id}&featureKey=${findFeature.key}',
                      text: (findFeature != null ? findFeature.name : ''))),
          findFeature != null
              ? activeTag(findFeature, fv, null)
              : inactiveTag(findFeature, null),
          Container(
              padding: EdgeInsets.fromLTRB(0, 10, 0, 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(_findApplication(fv, featuresList) != null
                      ? _findApplication(fv, featuresList).name
                      : ''),
                  Text(
                      _findPortfolio(fv, featuresList, bloc) != null
                          ? _findPortfolio(fv, featuresList, bloc).name
                          : '',
                      style: Theme.of(context).textTheme.caption),
                ],
              )),
          Text(
              _findEnvironment(fv, featuresList) != null
                  ? _findEnvironment(fv, featuresList).name
                  : '',
              style: Theme.of(context).textTheme.bodyText2),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(fv.whoUpdated != null ? fv.whoUpdated.name : '',
                  style: Theme.of(context).textTheme.bodyText2),
              Text(fv.whenUpdated != null ? _whenUpdated(fv.whenUpdated) : '',
                  style: Theme.of(context).textTheme.caption)
            ],
          ),
          Text(fv.whatUpdated != null ? fv.whatUpdated : ' ',
              style: Theme.of(context).textTheme.bodyText2),
        ]);
  }

  Feature _findFeature(
      FeatureValue fv, EnvironmentFeaturesResult featuresList) {
    return featuresList.features
        .firstWhere((feature) => feature.key == fv.key, orElse: () => null);
  }

  Application _findApplication(
      FeatureValue fv, EnvironmentFeaturesResult featuresList) {
    String appId = featuresList.environments
        .firstWhere((env) => env.id == fv.environmentId)
        .applicationId;
    return featuresList.applications
        .firstWhere((app) => app.id == appId, orElse: () => null);
  }

  Environment _findEnvironment(
      FeatureValue fv, EnvironmentFeaturesResult featuresList) {
    return featuresList.environments
        .firstWhere((env) => env.id == fv.environmentId);
  }

  Portfolio _findPortfolio(FeatureValue fv,
      EnvironmentFeaturesResult featuresList, FeaturesLatestBloc bloc) {
    String portfolioId = _findApplication(fv, featuresList).portfolioId;
    return bloc.portfolios.firstWhere(
        (portfolio) => portfolio.id == portfolioId,
        orElse: () => null);
  }

  String _whenUpdated(DateTime whenUpdated) {
    return timeago.format(whenUpdated);
  }
}
