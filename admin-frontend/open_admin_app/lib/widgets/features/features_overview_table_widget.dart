import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/features/environments_features_list_view.dart';
import 'package:open_admin_app/widgets/features/feature_names_left_panel.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_dashboard_constants.dart';
import 'feature_pagination_widget.dart';
import 'hidden_environment_list.dart';
import 'per_application_features_bloc.dart';

final _log = Logger('FeaturesOverviewTable');

class TabSelectedBloc implements Bloc {
  final _groupingSelected = BehaviorSubject.seeded(featureGroupDefault);
  final PerApplicationFeaturesBloc featureStatusBloc;

  late StreamSubscription<Feature> publishNewFeatureStream;

  TabSelectedBloc(this.featureStatusBloc) {
    // list for new features and swap to the right tab if we need to. We may still
    // not see it as we have no way of scrolling from here
    publishNewFeatureStream =
        featureStatusBloc.publishNewFeatureStream.listen((feature) {
      final fg = featureGroups.firstWhereOrNull(
          (grouping) => grouping.types.contains(feature.valueType));

      if (fg != null) {
        swapTab(fg);
      }
    });
  }

  Stream<FeatureGrouping> get currentGrouping => _groupingSelected.stream;

  // this follows the same pattern as PerApplicationFeaturesBloc
  Map<FeatureGrouping, FeaturesOnThisTabTrackerBloc> _featureBlocMap = {};

  FeaturesOnThisTabTrackerBloc featuresBloc(FeatureGrouping grouping) {
    return _featureBlocMap.putIfAbsent(grouping,
        () => FeaturesOnThisTabTrackerBloc(grouping, featureStatusBloc));
  }

  FeaturesOnThisTabTrackerBloc currentFeaturesBloc() {
    return featuresBloc(_groupingSelected.value!);
  }

  void swapTab(FeatureGrouping grouping) {
    _groupingSelected.add(grouping);
  }

  @override
  void dispose() {
    _groupingSelected.close();
    _featureBlocMap.values.forEach((bloc) {
      bloc.dispose();
    });
  }
}

class TabParentWidget extends StatelessWidget {
  const TabParentWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabSelectedBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _FeatureTabsHeader(),
        const HiddenEnvironmentsList(),
        StreamBuilder<FeatureGrouping?>(
            stream: bloc.currentGrouping,
            builder: (context, snapshot) {
              print("swap tab ${snapshot.data?.types}");
              if (!snapshot.hasData) {
                return const SizedBox.shrink();
              }

              return FeaturesOverviewTableWidget(
                grouping: snapshot.data!,
              );
            }),
      ],
    );
  }
}

class FeaturesOverviewTableWidget extends StatelessWidget {
  final FeatureGrouping grouping;

  const FeaturesOverviewTableWidget({Key? key, required this.grouping})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc =
        BlocProvider.of<TabSelectedBloc>(context).featuresBloc(grouping);

    try {
      return StreamBuilder<FeaturesByType?>(
          stream: bloc.featuresStream,
          builder: (context, snapshot) {
            if (!snapshot.hasData || snapshot.data!.isEmpty) {
              return const SizedBox.shrink();
            }

            final appFeatureValues = snapshot.data!.applicationFeatureValues;

            if (appFeatureValues.environments.isEmpty) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const <Widget>[
                  NoEnvironmentMessage(),
                ],
              );
            }

            if (appFeatureValues.features.isEmpty) {
              return const NoFeaturesMessage();
            }

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Card(
                  child: _FeatureTabsBodyHolder(bloc: bloc),
                ),
              ],
            );
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return const SizedBox.shrink();
    }
  }
}

class _FeatureTabsBodyHolder extends StatelessWidget {
  final FeaturesOnThisTabTrackerBloc bloc;

  const _FeatureTabsBodyHolder({Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: Column(
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
                        child: FeaturePaginationWidget(grouping: bloc.grouping)),
                  ...bloc.features.map(
                    (f) {
                      return FeatureNamesLeftPanel(tabsBloc: bloc, feature: f);
                    },
                  ).toList(),
                ])),
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
          children: [
            _FeatureTab(
                text: 'Standard Flags',
                subtext: '(Boolean)',
                icon: Icons.flag,
                state: featureGroupFlags,
                color: Colors.green),
            _FeatureTab(
                text: 'Non-binary Flags',
                subtext: '(String / Number)',
                icon: Icons.code,
                state: featureGroupValues,
                color: Colors.blue),
            _FeatureTab(
                text: 'Remote Configuration',
                subtext: '(JSON)',
                icon: Icons.device_hub,
                state: featureGroupConfig,
                color: Colors.orange),
          ],
        ),
      ),
    );
  }
}

class _FeatureTab extends StatelessWidget {
  final String text;
  final String subtext;
  final IconData icon;
  final FeatureGrouping state;

  final Color color;

  const _FeatureTab(
      {Key? key,
      required this.text,
      required this.icon,
      required this.state,
      required this.color,
      required this.subtext})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabSelectedBloc>(context);

    return Container(
        padding: const EdgeInsets.all(8.0),
        child: InkWell(
            canRequestFocus: false,
            mouseCursor: SystemMouseCursors.click,
            borderRadius: BorderRadius.circular(16.0),
            onTap: () {
              bloc.swapTab(state);
            },
            child: StreamBuilder<FeatureGrouping?>(
              stream: bloc.currentGrouping,
              builder: (context, snapshot) {
                return Container(
                    padding:
                        const EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
                    decoration: BoxDecoration(
                      borderRadius: const BorderRadius.all(Radius.circular(16.0)),
                      color:
                        snapshot.data == state ?
                          Theme.of(context).primaryColorLight : Colors.transparent,
                    ),
                    child: Row(children: <Widget>[
                      Icon(icon, color: color, size: 20.0),
                      const SizedBox(width: 4.0),
                      Text(text, style: Theme.of(context).textTheme.subtitle1),
                      const SizedBox(width: 2.0),
                      Text(subtext, style: Theme.of(context).textTheme.caption),
                    ]));
              }
            )));
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
              stream: bloc.mrClient.streamValley.currentPortfolioStream,
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
