import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/link_to_applications_page.dart';
import 'package:open_admin_app/widgets/features/edit-feature/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/features_data_table.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class FeatureStatusRoute extends StatefulWidget {
  final bool createFeature;

  const FeatureStatusRoute({Key? key, required this.createFeature})
      : super(key: key);

  @override
  State<StatefulWidget> createState() => _FeatureStatusState();
}

class _FeatureStatusState extends State<FeatureStatusRoute> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    FHAnalytics.sendWindowPath();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
            padding: const EdgeInsets.fromLTRB(0, 8, 30, 10),
            child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  const _FeaturesOverviewHeader(),
                  StreamBuilder<List<Application>?>(
                      stream: bloc.applications,
                      builder: (context, snapshot) {
                        if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                          return Column(
                            children: [
                              Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Row(
                                  children: [
                                    ApplicationDropDown(
                                        applications: snapshot.data!,
                                        bloc: bloc),
                                    const SizedBox(width: 16.0),
                                    CreateFeatureButton(bloc: bloc)
                                  ],
                                ),
                              ),
                              const FHPageDivider(),
                              const SizedBox(height: 16.0),
                              FeaturesDataTable(bloc: bloc)
                            ],
                          );
                        }
                        if (snapshot.hasData && snapshot.data!.isEmpty) {
                          return StreamBuilder<ReleasedPortfolio?>(
                              stream: bloc
                                  .mrClient.streamValley.currentPortfolioStream,
                              builder: (context, snapshot) {
                                if (snapshot.hasData &&
                                    snapshot
                                        .data!.currentPortfolioOrSuperAdmin) {
                                  return Row(
                                    children: <Widget>[
                                      SelectableText(
                                          'There are no applications in this portfolio',
                                          style: Theme.of(context)
                                              .textTheme
                                              .caption),
                                      const Padding(
                                        padding: EdgeInsets.only(left: 8.0),
                                        child: LinkToApplicationsPage(),
                                      ),
                                    ],
                                  );
                                } else {
                                  return SelectableText(
                                      "Either there are no applications in this portfolio or you don't have access to any of the applications.\n"
                                      'Please contact your administrator.',
                                      style:
                                          Theme.of(context).textTheme.caption);
                                }
                              });
                        }
                        return const SizedBox.shrink();
                      }),
                ])),
      ],
    );
  }
}

class _FeaturesOverviewHeader extends StatelessWidget {
  const _FeaturesOverviewHeader({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const FHHeader(
      title: 'Features console',
    );
  }
}

class CreateFeatureButton extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;

  const CreateFeatureButton({
    Key? key,
    required this.bloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: bloc.mrClient.streamValley.currentAppIdStream,
        builder: (context, snapshot) {
          final canEdit = bloc.mrClient.personState
              .personCanEditFeaturesForApplication(snapshot.data);
          return !canEdit
              ? const SizedBox.shrink()
              : ElevatedButton.icon(
                  // keepCase: true,
                  onPressed: () =>
                      bloc.mrClient.addOverlay((BuildContext context) {
                        return CreateFeatureDialogWidget(
                          bloc: bloc,
                        );
                      }),
                  icon: const Icon(Icons.add),
                  label: const Text('Create New Feature'));

        });
  }
}
