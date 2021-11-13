import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:logging/logging.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/features/environments_features_list_view.dart';
import 'package:open_admin_app/widgets/features/feature_names_left_panel.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';

import 'feature_dashboard_constants.dart';
import 'hidden_environment_list.dart';
import 'per_application_features_bloc.dart';

final _log = Logger('FeaturesOverviewTable');

class FeaturesOverviewTableWidget extends StatelessWidget {
  const FeaturesOverviewTableWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    try {
      return StreamBuilder<FeatureStatusFeatures?>(
          stream: bloc.appFeatureValues,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return const SizedBox.shrink();
            }

            if (snapshot.hasData &&
                snapshot.data!.sortedByNameEnvironmentIds.isEmpty) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const <Widget>[
                  NoEnvironmentMessage(),
                ],
              );
            }
            if (snapshot.hasData &&
                snapshot.data!.applicationFeatureValues.features.isEmpty) {
              return const NoFeaturesMessage();
            }

            if (snapshot.hasData) {
              return TabsView(
                featureStatus: snapshot.data!,
                applicationId: bloc.applicationId!,
                bloc: bloc,
              );
            } else {
              return const NoFeaturesMessage();
            }
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return const SizedBox.shrink();
    }
  }
}

class TabsView extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;
  final String applicationId;
  final PerApplicationFeaturesBloc bloc;

  const TabsView(
      {Key? key,
      required this.featureStatus,
      required this.applicationId,
      required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
        creator: (_c, _b) => FeaturesOnThisTabTrackerBloc(
            featureStatus,
            applicationId,
            BlocProvider.of<ManagementRepositoryClientBloc>(context),
            bloc),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _FeatureTabsHeader(),
            Card(
              child: Column(
                children: [
                  const HiddenEnvironmentsList(),
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
    final bloc = BlocProvider.of<FeaturesOnThisTabTrackerBloc>(context);

    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: StreamBuilder<TabsState>(
              stream: bloc.currentTab,
              builder: (context, snapshot) {
                return Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (bloc.features.isNotEmpty)
                        Container(
                            color: Theme.of(context).highlightColor,
                            height: headerHeight,
                            width: MediaQuery.of(context).size.width > 600
                                ? 260.0
                                : 130,
                            padding: const EdgeInsets.only(left: 8.0),
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
    final ScrollController controller = ScrollController();

    return ScrollConfiguration(
      behavior: CustomScrollBehavior(),
      child: SingleChildScrollView(
        controller: controller,
        scrollDirection: Axis.horizontal,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: const [
            _FeatureTab(
                text: 'Feature Flags',
                icon: Icons.flag,
                state: TabsState.featureFlags,
                color: Colors.green),
            _FeatureTab(
                text: 'Feature Values',
                icon: Icons.code,
                state: TabsState.featureValues,
                color: Colors.blue),
            _FeatureTab(
                text: 'Configurations',
                icon: Icons.device_hub,
                state: TabsState.configurations,
                color: Colors.orange),
          ],
        ),
      ),
    );
  }
}

class _FeatureTab extends StatelessWidget {
  final String text;
  final IconData icon;
  final TabsState state;

  final Color color;

  const _FeatureTab(
      {Key? key,
      required this.text,
      required this.icon,
      required this.state,
      required this.color})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeaturesOnThisTabTrackerBloc>(context);

    return StreamBuilder<TabsState>(
        stream: bloc.currentTab,
        builder: (context, snapshot) {
          return Container(
              padding: const EdgeInsets.all(8.0),
              child: InkWell(
                  canRequestFocus: false,
                  mouseCursor: SystemMouseCursors.click,
                  borderRadius: BorderRadius.circular(16.0),
                  onTap: () {
                    bloc.swapTab(state);
                  },
                  child: Container(
                      padding: const EdgeInsets.symmetric(
                          vertical: 6.0, horizontal: 12.0),
                      decoration: BoxDecoration(
                        borderRadius:
                            const BorderRadius.all(Radius.circular(16.0)),
                        color: state == snapshot.data
                            ? Theme.of(context).primaryColorLight
                            : Colors.transparent,
                      ),
                      child: Row(children: <Widget>[
                        Icon(icon, color: color, size: 20.0),
                        const SizedBox(width: 4.0),
                        Text(text,
                            style: Theme.of(context).textTheme.subtitle1),
                      ]))));
        });
  }
}

class NoEnvironmentMessage extends StatelessWidget {
  const NoEnvironmentMessage({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    return Padding(
      padding: const EdgeInsets.only(left: 12.0),
      child: Row(
        children: <Widget>[
          Text(
              'Either there are no environments defined for this application or you don\'t have permissions to access any of them',
              style: Theme.of(context).textTheme.caption),
          StreamBuilder<ReleasedPortfolio?>(
              stream: bloc.mrClient.personState.isCurrentPortfolioOrSuperAdmin,
              builder: (context, snapshot) {
                if (snapshot.hasData &&
                    snapshot.data!.currentPortfolioOrSuperAdmin) {
                  return Padding(
                    padding: const EdgeInsets.only(left: 8.0),
                    child: FHUnderlineButton(
                        title: 'Go to environments settings',
                        keepCase: true,
                        onPressed: () => ManagementRepositoryClientBloc.router
                                .navigateTo(context, '/app-settings', params: {
                              'id': [bloc.applicationId!],
                              'tab': ['environments']
                            })),
                  );
                } else {
                  return Container();
                }
              })
        ],
      ),
    );
  }
}

class NoFeaturesMessage extends StatelessWidget {
  const NoFeaturesMessage({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.only(top: 16.0),
      child: Column(
        children: <Widget>[
          Text('There are no features defined for this application',
              style: Theme.of(context).textTheme.caption),
        ],
      ),
    );
  }
}
