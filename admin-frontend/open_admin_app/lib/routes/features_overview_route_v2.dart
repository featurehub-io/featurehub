import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_accent.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/link_to_applications_page.dart';
import 'package:open_admin_app/widgets/features/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/create_update_feature_dialog_widgetV2.dart';
import 'package:open_admin_app/widgets/features/experiment_data_table.dart';
import 'package:open_admin_app/widgets/features/features_overview_table_widget.dart';
import 'package:open_admin_app/widgets/features/features_overview_table_widgetv2.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class FeatureStatusRouteV2 extends StatefulWidget {
  final bool createFeature;

  const FeatureStatusRouteV2({Key? key, required this.createFeature})
      : super(key: key);

  @override
  State<StatefulWidget> createState() => _FeatureStatusState();
}

class _FeatureStatusState extends State<FeatureStatusRouteV2> {
  // PerApplicationFeaturesBloc? bloc;
  //
  // @override
  // void initState() {
  //   super.initState();
  //
  //   bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
  // }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(context, bloc),
        const FHPageDivider(),
        const SizedBox(height: 16.0),
        const FeaturesOverviewTableWidgetV2()
      ],
    );
  }

  Widget _headerRow(BuildContext context, PerApplicationFeaturesBloc bloc) {
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 30, 10),
        child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const _FeaturesOverviewHeader(),
              FittedBox(
                child: Row(
                  children: [
                    _filterRow(context, bloc),
                  ],
                ),
              ),
            ]));
  }

  Widget _filterRow(BuildContext context, PerApplicationFeaturesBloc bloc) {
    return Column(
      children: <Widget>[
        Container(
          // color: Colors.red,
          padding: const EdgeInsets.fromLTRB(12, 16, 16, 16),
          child: StreamBuilder<List<Application>?>(
              stream: bloc.applications,
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                  return ApplicationDropDown(
                      applications: snapshot.data!, bloc: bloc);
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
                                  style: Theme.of(context).textTheme.caption),
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
                              style: Theme.of(context).textTheme.caption);
                        }
                      });
                }
                return const SizedBox.shrink();
              }),
        ),
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
  final FeaturesDataSource featuresDataSource;

  const CreateFeatureButton({
    Key? key,
    required this.bloc, required this.featuresDataSource,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: bloc.mrClient.streamValley.currentAppIdStream,
        builder: (context, snapshot) {
          final canEdit = bloc.mrClient.personState
              .personCanEditFeaturesForCurrentApplication(snapshot.data);
          return !canEdit
              ? const SizedBox.shrink()
              : ElevatedButton(
                  // keepCase: true,
                  onPressed: () =>
                      bloc.mrClient.addOverlay((BuildContext context) {
                    //return null;
                    return CreateFeatureDialogWidgetV2(
                      bloc: bloc,
                      featuresDataSource: featuresDataSource
                    );
                  }), child: Text('Create new feature')
                );
        });
  }
}
