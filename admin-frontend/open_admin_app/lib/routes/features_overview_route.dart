import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/link_to_applications_page.dart';
import 'package:open_admin_app/widgets/features/edit-feature/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/features_data_table.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/portfolio/feature_filter_bloc.dart';

class FeatureStatusRoute extends StatefulWidget {
  final bool createFeature;

  const FeatureStatusRoute({super.key, required this.createFeature});

  @override
  State<StatefulWidget> createState() => _FeatureStatusState();
}

class _FeatureStatusState extends State<FeatureStatusRoute> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    FHAnalytics.sendScreenView("features-dashboard");

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              FHHeader(
                title: AppLocalizations.of(context)!.featuresConsole,
                children: [
                  FHExternalLinkWidget(
                    tooltipMessage: AppLocalizations.of(context)!.viewDocumentation,
                    link:
                        "https://docs.featurehub.io/featurehub/latest/features.html",
                    icon: const Icon(Icons.arrow_outward_outlined),
                    label: AppLocalizations.of(context)!.featuresDocumentation,
                  )
                ],
              ),
              StreamBuilder<List<Application>?>(
                  stream: bloc.applications,
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
                          stream:
                              bloc.mrClient.streamValley.currentPortfolioStream,
                          builder: (context, snapshot) {
                            if (snapshot.hasData &&
                                snapshot.data!.currentPortfolioOrSuperAdmin) {
                              return Row(
                                children: <Widget>[
                                  SelectableText(
                                      AppLocalizations.of(context)!.noApplicationsInPortfolio,
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodySmall),
                                  const Padding(
                                    padding: EdgeInsets.only(left: 8.0),
                                    child: LinkToApplicationsPage(),
                                  ),
                                ],
                              );
                            } else {
                              return SelectableText(
                                  AppLocalizations.of(context)!.noApplicationsAccessMessage,
                                  style: Theme.of(context).textTheme.bodySmall);
                            }
                          });
                    }
                    return const SizedBox.shrink();
                  }),
            ]),
      ],
    );
  }
}

class CreateFeatureButton extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;

  const CreateFeatureButton({
    super.key,
    required this.bloc,
  });

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: bloc.mrClient.streamValley.currentAppIdStream,
        builder: (context, snapshot) {
          final canEdit = bloc.mrClient.personState
              .personCanCreateFeaturesForApplication(snapshot.data);
          return !canEdit
              ? const SizedBox.shrink()
              : FilledButton.icon(
                  // keepCase: true,
                  onPressed: () {
                    bloc.mrClient.addOverlay((BuildContext context) => createFeatureDialog(bloc, null)
                    );
                  },
                  icon: const Icon(Icons.add),
                  label: Text(AppLocalizations.of(context)!.createNewFeature));
        });
  }
}
