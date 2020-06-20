import 'package:app_singleapp/widgets/apps/app_delete_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/app_update_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/manage_app_bloc.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
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
                if (bloc.mrClient.userIsAnyPortfolioOrSuperAdmin)
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
}

class _ApplicationsCardsList extends StatelessWidget {
  final ManageAppBloc bloc;

  const _ApplicationsCardsList({Key key, @required this.bloc})
      : assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<List<Application>>(
        stream: bloc.mrClient.streamValley.currentPortfolioApplicationsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return SizedBox.shrink();
          }

          return Wrap(
            direction: Axis.horizontal,
            crossAxisAlignment: WrapCrossAlignment.start,
            children: snapshot.data
                .map((app) => _ApplicationCard(application: app, bloc: bloc))
                .toList(),
          );
        });
  }
}

class _ApplicationCard extends StatelessWidget {
  final Application application;
  final ManageAppBloc bloc;

  const _ApplicationCard(
      {Key key, @required this.application, @required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Container(
        color: Theme
            .of(context)
            .backgroundColor,
        width: 240,
        height: 130,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(application.name,
                  style: Theme
                      .of(context)
                      .textTheme
                      .subtitle1
                      .copyWith(color: Theme
                      .of(context)
                      .primaryColor)),
              Column(
                children: [
                  FHPageDivider(),
                  SizedBox(height: 4.0),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.start,
                        children: [
                          if (application.environments.length
                              .toString()
                              .isNotEmpty)
                            Column(
                              children: [
                                Text(
                                    application.environments.length.toString()),
                                Icon(AntDesign.bars, size: 16.0),
                              ],
                            ),
                          if (application.features
                              .where((element) =>
                          element.valueType == FeatureValueType.BOOLEAN)
                              .toList()
                              .isNotEmpty)
                            Column(
                              children: [
                                Text(application.features
                                    .where((element) =>
                                element.valueType ==
                                    FeatureValueType.BOOLEAN)
                                    .toList()
                                    .length
                                    .toString()),
                                Icon(Icons.flag, size: 16.0),
                              ],
                            ),
                          if ((application.features
                              .where((element) =>
                          element.valueType ==
                              FeatureValueType.STRING)
                              .toList()
                              .isNotEmpty) ||
                              (application.features
                                  .where((element) =>
                              element.valueType ==
                                  FeatureValueType.NUMBER)
                                  .toList()
                                  .isNotEmpty))
                            Column(
                              children: [
                                Text(((application.features
                                    .where((element) =>
                                element.valueType ==
                                    FeatureValueType.STRING)
                                    .toList()
                                    .length) +
                                    (application.features
                                        .where((element) =>
                                    element.valueType ==
                                        FeatureValueType.NUMBER)
                                        .toList()
                                        .length))
                                    .toString()),
                                Icon(Icons.code, size: 16.0),
                              ],
                            ),
                          if (application.features
                              .where((element) =>
                          element.valueType == FeatureValueType.JSON)
                              .toList()
                              .isNotEmpty)
                            Column(
                              children: [
                                Text(application.features
                                    .where((element) =>
                                element.valueType ==
                                    FeatureValueType.JSON)
                                    .toList()
                                    .length
                                    .toString()),
                                Icon(Icons.device_hub, size: 16.0),
                              ],
                            ),
                        ],
                      ),
                      if (bloc.mrClient.userIsAnyPortfolioOrSuperAdmin)
                        _AdminActions(bloc: bloc)
                    ],
                  ),
                ],
              )
            ],
          ),
        ),
      ),
    );
  }
}

class _AdminActions extends StatelessWidget {
  final ManageAppBloc bloc;

  const _AdminActions({Key key, @required this.bloc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(left: 10),
      child: Row(children: <Widget>[
        StreamBuilder<String>(
            stream: bloc.mrClient.streamValley.currentAppIdStream,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Row(children: <Widget>[
                  FHIconButton(
                      width: 30.0,
                      icon: Icon(Icons.edit,
                          color: Theme.of(context).buttonColor, size: 16.0),
                      onPressed: () => bloc.mrClient.addOverlay(
                          (BuildContext context) => AppUpdateDialogWidget(
                                bloc: bloc,
                                application: bloc.application,
                              ))),
                  FHIconButton(
                      width: 30.0,
                      icon: Icon(Icons.delete,
                          color: Theme
                              .of(context)
                              .buttonColor, size: 16.0),
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
