import 'package:app_singleapp/widgets/apps/app_delete_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/app_update_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/manage_app_bloc.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class AppsRoute extends StatefulWidget {
  @override
  _AppsRouteState createState() => _AppsRouteState();
}

class _AppsRouteState extends State<AppsRoute> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: [
                Container(
                  padding: EdgeInsets.only(bottom: 10),
                  child: FHHeader(
                    title: 'Applications',
                    children: <Widget>[],
                  ),
                ),
                Container(
                    child: FHIconTextButton(
                  iconData: Icons.add,
                  keepCase: true,
                  label: 'Create new application',
                  onPressed: () =>
                      bloc.mrClient.addOverlay((BuildContext context) {
                    return AppUpdateDialogWidget(
                      bloc: bloc,
                    );
                  }),
                )),
              ],
            ),
            FHPageDivider(),
            _ApplicationsCardsList(
              bloc: bloc,
            )
          ],
        ));
  }

  Widget _getAdminActions(ManageAppBloc bloc) {
    return Container(
      padding: EdgeInsets.only(left: 10),
      child: Row(children: <Widget>[
        StreamBuilder<String>(
            stream: bloc.mrClient.streamValley.currentAppIdStream,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Row(children: <Widget>[
                  FHIconButton(
                      icon: Icon(Icons.edit,
                          color: Theme.of(context).buttonColor),
                      onPressed: () => bloc.mrClient.addOverlay(
                          (BuildContext context) => AppUpdateDialogWidget(
                                bloc: bloc,
                                application: bloc.application,
                              ))),
                  FHIconButton(
                      icon: Icon(Icons.delete,
                          color: Theme.of(context).buttonColor),
                      onPressed: () =>
                          bloc.mrClient.addOverlay((BuildContext context) {
                            return AppDeleteDialogWidget(
                              bloc: bloc,
                              application: bloc.application,
                            );
                          }))
                ]);
              } else {
                return Container();
              }
            }),
      ]),
    );
  }
}

class _ApplicationsCardsList extends StatelessWidget {
  final ManageAppBloc bloc;

  const _ApplicationsCardsList({Key key, @required this.bloc})
      : assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Portfolio>(
        stream: bloc.mrClient.streamValley.currentPortfolioStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return SizedBox.shrink();
          }

          return Wrap(
            direction: Axis.horizontal,
            crossAxisAlignment: WrapCrossAlignment.start,
            children: snapshot.data.applications
                .map((app) => _ApplicationCard(
                      application: app,
                    ))
                .toList(),
          );
        });
  }
}

class _ApplicationCard extends StatelessWidget {
  final Application application;

  const _ApplicationCard({Key key, this.application}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    List<Color> _colors = [Color(0xff95F0DA), Color(0xff6DD3F4)];
    List<double> _stops = [0.0, 0.7];
    return Card(
      child: Container(
        decoration: BoxDecoration(
            gradient: LinearGradient(
          colors: _colors,
          stops: _stops,
        )),
        width: 240,
        height: 130,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(application.name,
                  style: Theme.of(context)
                      .textTheme
                      .subtitle1
                      .copyWith(color: Theme.of(context).primaryColor)),
            ],
          ),
        ),
      ),
    );
  }
}
