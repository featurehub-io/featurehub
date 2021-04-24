import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/widgets/common/application_drop_down.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_accent.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/link_to_applications_page.dart';
import 'package:app_singleapp/widgets/features/create-update-feature-dialog-widget.dart';
import 'package:app_singleapp/widgets/features/features_overview_table_widget.dart';
import 'package:app_singleapp/widgets/features/per_application_features_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class FeatureStatusRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(child: _FeatureStatusWidget());
  }
}

class _FeatureStatusWidget extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => _FeatureStatusState();
}

class _FeatureStatusState extends State<_FeatureStatusWidget> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(context, bloc),
        FHPageDivider(),
        SizedBox(height: 16.0),
        FeaturesOverviewTableWidget()
      ],
    );
  }

  Widget _headerRow(BuildContext context, PerApplicationFeaturesBloc bloc) {
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 0, 30, 10),
        child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              _FeaturesOverviewHeader(),
              FittedBox(
                child: Row(
                  children: [
                    _filterRow(context, bloc),
                    _CreateFeatureButton(bloc: bloc),
                  ],
                ),
              ),
            ]));
  }

  Widget _filterRow(BuildContext context, PerApplicationFeaturesBloc bloc) {
    return Column(
      children: <Widget>[
        Container(
          padding: const EdgeInsets.fromLTRB(12, 16, 16, 16),
          child: StreamBuilder<List<Application>?>(
              stream: bloc.applications,
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                  return ApplicationDropDown(
                      applications: snapshot.data!, bloc: bloc);
                }
                if (snapshot.hasData && snapshot.data!.isEmpty) {
                  return StreamBuilder<ReleasedPortfolio>(
                      stream: bloc
                          .mrClient.personState.isCurrentPortfolioOrSuperAdmin,
                      builder: (context, snapshot) {
                        if (snapshot.hasData &&
                            snapshot.data!.currentPortfolioOrSuperAdmin) {
                          return Row(
                            children: <Widget>[
                              Text(
                                  'There are no applications in this portfolio',
                                  style: Theme.of(context).textTheme.caption),
                              LinkToApplicationsPage(),
                            ],
                          );
                        } else {
                          return Text(
                              "Either there are no applications in this portfolio or you don't have access to any of the applications.\n"
                              'Please contact your administrator.',
                              style: Theme.of(context).textTheme.caption);
                        }
                      });
                }
                return Container();
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
    return FHHeader(
      title: 'Features console',
    );
  }
}

class _CreateFeatureButton extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;

  const _CreateFeatureButton({
    Key? key,
    required this.bloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: bloc.mrClient.streamValley.currentAppIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData && snapshot.data != null) {
            return FutureBuilder<bool>(
                future: bloc.mrClient.personState
                    .personCanEditFeaturesForCurrentApplication(snapshot.data),
                builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
                  if (snapshot.data == true || bloc.mrClient.userIsSuperAdmin) {
                    return FHFlatButtonAccent(
                      keepCase: true,
                      title: 'Create new feature',
                      onPressed: () =>
                          bloc.mrClient.addOverlay((BuildContext context) {
                        //return null;
                        return CreateFeatureDialogWidget(
                          bloc: bloc,
                        );
                      }),
                    );
                  }

                  return SizedBox.shrink();
                });
          }
          return SizedBox.shrink();
        });
  }
}
