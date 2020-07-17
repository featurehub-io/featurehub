import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/environments_features_list_view.dart';
import 'package:app_singleapp/widgets/features/feature_names_left_panel.dart';
import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:logging/logging.dart';

import 'feature_dashboard_constants.dart';
import 'feature_status_bloc.dart';
import 'hidden_environment_list.dart';

final _log = Logger('FeaturesOverviewTable');

class FeaturesOverviewTableWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    try {
      return StreamBuilder<FeatureStatusFeatures>(
          stream: bloc.appFeatureValues,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return SizedBox.shrink();
            }

            if (snapshot.hasData &&
                snapshot.data.sortedByNameEnvironmentIds.isEmpty) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  NoEnvironmentMessage(),
                ],
              );
            }
            if (snapshot.hasData &&
                snapshot.data.applicationFeatureValues.features.isEmpty) {
              return NoFeaturesMessage();
            }

            if (snapshot.hasData) {
              return TabsView(
                featureStatus: snapshot.data,
                applicationId: bloc.applicationId,
                bloc: bloc,
              );
            } else {
              return NoFeaturesMessage();
            }
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return SizedBox.shrink();
    }
  }
}

class TabsView extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;
  final String applicationId;
  final FeatureStatusBloc bloc;

  const TabsView(
      {Key key,
      @required this.featureStatus,
      @required this.applicationId,
      @required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
        creator: (_c, _b) => TabsBloc(featureStatus, applicationId,
            BlocProvider.of<ManagementRepositoryClientBloc>(context), bloc),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _FeatureTabsHeader(),
            Card(
              child: Column(
                children: [
                  HiddenEnvironmentsList(),
                  _FeatureTabsBodyHolder(),
                ],
              ),
            ),
          ],
        ));
  }
}

class _FeatureTabsBodyHolder extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabsBloc>(context);

    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: Container(
//            color: Theme.of(context).highlightColor,
            child: StreamBuilder<TabsState>(
                stream: bloc.currentTab,
                builder: (context, snapshot) {
                  return Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                            height: headerHeight,
                            padding: EdgeInsets.only(left: 8.0),
                            child: Text('',
                                style: Theme.of(context).textTheme.caption)),
                        ...bloc.features.map(
                          (f) {
                            return FeatureNamesLeftPanel(
                                tabsBloc: bloc, feature: f);
                          },
                        ).toList(),
                      ]);
                }),
          ),
        ),
        Flexible(
          child: EnvironmentsAndFeatureValuesListView(bloc: bloc),
        )
      ],
    );
  }
}

class _FeatureTabsHeader extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        _FeatureTab(
            text: 'Feature Flags',
            icon: Icons.flag,
            state: TabsState.FLAGS,
            color: Colors.green),
        _FeatureTab(
            text: 'Feature Values',
            icon: Icons.code,
            state: TabsState.VALUES,
            color: Colors.blue),
        _FeatureTab(
            text: 'Configurations',
            icon: Icons.device_hub,
            state: TabsState.CONFIGURATIONS,
            color: Colors.orange),
      ],
    );
  }
}

class _FeatureTab extends StatelessWidget {
  final String text;
  final IconData icon;
  final TabsState state;

  final color;

  const _FeatureTab(
      {Key key,
      @required this.text,
      @required this.icon,
      @required this.state,
      @required this.color})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabsBloc>(context);

    return StreamBuilder<TabsState>(
        stream: bloc.currentTab,
        builder: (context, snapshot) {
          return CustomCursor(
            child: Container(
                padding: const EdgeInsets.all(8.0),
                child: InkWell(
                    canRequestFocus: false,
                    mouseCursor: SystemMouseCursors.click,
                    borderRadius: BorderRadius.circular(16.0),
                    onTap: () {
                      bloc.swapTab(state);
                    },
                    child: Container(
                        padding: EdgeInsets.symmetric(
                            vertical: 6.0, horizontal: 12.0),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.all(Radius.circular(16.0)),
                          color: state == snapshot.data
                              ? Theme.of(context).primaryColorLight
                              : Colors.transparent,
                        ),
                        child: Row(children: <Widget>[
                          Icon(icon, color: color, size: 20.0),
                          SizedBox(width: 4.0),
                          Text(text,
                              style: Theme.of(context).textTheme.subtitle1),
                        ])))),
          );
        });
  }
}

class NoEnvironmentMessage extends StatelessWidget {
  const NoEnvironmentMessage({
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    return Row(
      children: <Widget>[
        Text(
            'Either there are no environments defined for this application or you don\'t have permissions to access any of them',
            style: Theme.of(context).textTheme.caption),
        StreamBuilder<ReleasedPortfolio>(
            stream: bloc.mrClient.personState.isCurrentPortfolioOrSuperAdmin,
            builder: (context, snapshot) {
              if (snapshot.hasData &&
                  snapshot.data.currentPortfolioOrSuperAdmin) {
                return FHFlatButtonTransparent(
                    title: 'Manage application',
                    keepCase: true,
                    onPressed: () => ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/manage-app',
                                transition: TransitionType.material,
                                params: {
                              'id': [bloc.applicationId],
                              'tab-name': ['environments']
                            }));
              } else {
                return Container();
              }
            })
      ],
    );
  }
}

class NoFeaturesMessage extends StatelessWidget {
  const NoFeaturesMessage({
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(top: 16.0),
      child: Column(
        children: <Widget>[
          Text('There are no features defined for this application',
              style: Theme.of(context).textTheme.caption),
        ],
      ),
    );
  }
}
