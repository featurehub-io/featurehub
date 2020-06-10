import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_accent.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/features/create-update-feature-dialog-widget.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:app_singleapp/widgets/features/features_overview_table_widget.dart';
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
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _filterRow(context, bloc),
        _headerRow(context, bloc),
        Container(
            decoration: BoxDecoration(
                border: Border(
                    bottom: BorderSide(color: Colors.black87, width: 0.5)))),
        FeaturesOverviewTableWidget()
      ],
    );
  }

  Widget _headerRow(BuildContext context, FeatureStatusBloc bloc) {
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 0, 30, 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            FHHeader(
              title: 'Features and Configuration Statuses',
            ),
            StreamBuilder<String>(
                stream: bloc.mrClient.streamValley.currentAppIdStream,
                builder: (context, snapshot) {
                  if (snapshot.hasData && snapshot.data != null) {
                    return FutureBuilder<bool>(
                        future: bloc.mrClient.personState
                            .personCanEditFeaturesForCurrentApplication(
                                snapshot.data),
                        builder: (BuildContext context,
                            AsyncSnapshot<bool> snapshot) {
                          if (snapshot.data == true ||
                              bloc.mrClient.userIsSuperAdmin) {
                            return Padding(
                              padding: const EdgeInsets.only(left: 52.0),
                              child: Container(
                                  child: FHFlatButtonAccent(
                                keepCase: true,
                                title: 'Create new feature',
                                onPressed: () => bloc.mrClient
                                    .addOverlay((BuildContext context) {
                                  //return null;
                                  return CreateFeatureDialogWidget(
                                    bloc: bloc,
                                  );
                                }),
                              )),
                            );
                          }

                          return SizedBox.shrink();
                        });
                  }
                  return SizedBox.shrink();
                }),
          ],
        ));
  }

  Widget _filterRow(BuildContext context, FeatureStatusBloc bloc) {
    return Column(
      children: <Widget>[
        Container(
          padding: const EdgeInsets.fromLTRB(12, 16, 16, 16),
          child: StreamBuilder<List<Application>>(
              stream: bloc.applications,
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data.isNotEmpty) {
                  return Container(
                      padding: EdgeInsets.all(4.0),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        border: Border.all(color: Colors.black87, width: 0.5),
                        borderRadius: BorderRadius.all(Radius.circular(2.0)),
                      ),
                      child: ApplicationDropDown(
                          applications: snapshot.data, bloc: bloc));
                }
                if (snapshot.hasData && snapshot.data.isEmpty) {
                  return Container(
                      padding: EdgeInsets.only(left: 30.0),
                      child: StreamBuilder<bool>(
                          stream: bloc.mrClient.personState
                              .isCurrentPortfolioOrSuperAdmin,
                          builder: (context, snapshot) {
                            if (snapshot.hasData && snapshot.data) {
                              return Row(
                                children: <Widget>[
                                  Text(
                                      'There are no applications in this portfolio',
                                      style:
                                          Theme.of(context).textTheme.caption),
                                  FHFlatButtonTransparent(
                                      title: 'Manage applications',
                                      keepCase: true,
                                      onPressed: () =>
                                          ManagementRepositoryClientBloc.router
                                              .navigateTo(
                                                  context, '/manage-app',
                                                  replace: true,
                                                  transition:
                                                      TransitionType.material)),
                                ],
                              );
                            } else {
                              return Text(
                                  "Either there are no applications in this portfolio or you don't have access to any of the applications.\n"
                                  'Please contact your administrator.',
                                  style: Theme.of(context).textTheme.caption);
                            }
                          }));
                }
                return Container();
              }),
        ),
      ],
    );
  }
}

class ApplicationDropDown extends StatefulWidget {
  final List<Application> applications;
  final FeatureStatusBloc bloc;

  const ApplicationDropDown({Key key, this.applications, this.bloc})
      : super(key: key);

  @override
  _ApplicationDropDownState createState() => _ApplicationDropDownState();
}

class _ApplicationDropDownState extends State<ApplicationDropDown> {
  @override
  Widget build(BuildContext context) {
    return Container(
      child: DropdownButtonHideUnderline(
        child: DropdownButton(
          isExpanded: false,
          isDense: true,
          items: widget.applications != null && widget.applications.isNotEmpty
              ? widget.applications.map((Application application) {
                  return DropdownMenuItem<String>(
                      value: application.id,
                      child: Text(application.name,
                          style: Theme.of(context).textTheme.bodyText2));
                }).toList()
              : null,
          hint: Text('Select application',
              style: Theme.of(context).textTheme.subtitle2),
          onChanged: (value) {
            setState(() {
              widget.bloc.applicationId = value;
              widget.bloc.mrClient.setCurrentAid(value);
              widget.bloc.addAppFeatureValuesToStream();
            });
          },
          value: widget.bloc.applicationId,
        ),
      ),
    );
  }
}
