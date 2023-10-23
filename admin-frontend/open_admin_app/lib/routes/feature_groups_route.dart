import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/feature-groups/environment_drop_down.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_card_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_update_dialog_widget.dart';

class FeatureGroupsRoute extends StatefulWidget {
  final bool createApp;

  const FeatureGroupsRoute({Key? key, required this.createApp})
      : super(key: key);

  @override
  FeatureGroupsRouteState createState() => FeatureGroupsRouteState();
}

class FeatureGroupsRouteState extends State<FeatureGroupsRoute> {
  FeatureGroupsBloc? bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<FeatureGroupsBloc>(context);
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureGroupsBloc>(context);
    FHAnalytics.sendScreenView("feature-groups");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          children: [
            const FHHeader(
              title: 'FeatureGroups',
            ),
            StreamBuilder<List<RoleType>>(
                stream: bloc.envRoleTypeStream,
                builder: (context, snapshot) {
                  if (snapshot.data != null &&
                      (snapshot.data!.contains(RoleType.CHANGE_VALUE))) {
                    return FilledButton.icon(
                      icon: const Icon(Icons.add),
                      label: const Text('Create feature group'),
                      onPressed: () => {
                        bloc.mrClient.addOverlay((BuildContext context) {
                          return FeatureGroupUpdateDialogWidget(
                            bloc: bloc,
                          );
                        })
                      },
                    );
                  } else {
                    return const SizedBox.shrink();
                  }
                }),
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        _FeatureGroupsCardsList(
          bloc: bloc,
        )
      ],
    );
  }
}

class _FeatureGroupsCardsList extends StatelessWidget {
  final FeatureGroupsBloc bloc;
  const _FeatureGroupsCardsList({Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        StreamBuilder<List<Application>?>(
            stream: bloc.currentApplicationsStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                          vertical: 16.0, horizontal: 8.0),
                      child: Wrap(
                        spacing: 16.0,
                        runSpacing: 16.0,
                        children: [
                          ApplicationDropDown(
                              applications: snapshot.data!, bloc: bloc),
                          EnvironmentDropDown(
                            bloc: bloc,
                            envId: bloc.currentEnvId,
                          )
                        ],
                      ),
                    ),
                    const FHPageDivider(),
                    const SizedBox(height: 16.0),
                  ],
                );
              }
              if (snapshot.hasData && snapshot.data!.isEmpty) {
                return StreamBuilder<ReleasedPortfolio?>(
                    stream: bloc.mrClient.streamValley.currentPortfolioStream,
                    builder: (context, snapshot) {
                      if (snapshot.hasData &&
                          snapshot.data!.currentPortfolioOrSuperAdmin) {
                        return Row(
                          children: <Widget>[
                            SelectableText(
                                'There are no applications in this portfolio',
                                style: Theme.of(context).textTheme.bodySmall),
                          ],
                        );
                      } else {
                        return SelectableText(
                            "Either there are no applications in this portfolio or you don't have access to any of the applications.\n"
                            'Please contact your administrator.',
                            style: Theme.of(context).textTheme.bodySmall);
                      }
                    });
              }
              return const SizedBox.shrink();
            }),
        StreamBuilder<List<FeatureGroupListGroup>>(
            stream: bloc.featureGroupsStream,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const FHLoadingIndicator();
              } else if (snapshot.connectionState == ConnectionState.active ||
                  snapshot.connectionState == ConnectionState.done) {
                if (snapshot.hasError) {
                  return const FHLoadingError();
                } else if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                  return Align(
                    alignment: Alignment.topLeft,
                    child: Wrap(
                      direction: Axis.horizontal,
                      crossAxisAlignment: WrapCrossAlignment.start,
                      children: snapshot.data!
                          .map((featureGroup) => FeatureGroupCard(
                                featureGroup: featureGroup,
                                bloc: bloc,
                              ))
                          .toList(),
                    ),
                  );
                }
              }
              return const SizedBox.shrink();
            }),
      ],
    );
  }
}
