import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/features/features_data_table.dart';
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
            } else {
              return FeaturesDataTable(bloc: bloc);
            }
            // else {
            //   return const NoFeaturesMessage();
            // }
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return const SizedBox.shrink();
    }
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
          SelectableText(
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
          SelectableText('There are no features defined for this application',
              style: Theme.of(context).textTheme.caption),
        ],
      ),
    );
  }
}
