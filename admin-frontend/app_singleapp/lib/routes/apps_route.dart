import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/common/stream_valley.dart';
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
                StreamBuilder<ReleasedPortfolio>(
                    stream: bloc
                        .mrClient.personState.isCurrentPortfolioOrSuperAdmin,
                    builder: (context, snapshot) {
                      if (snapshot.data != null &&
                          (snapshot.data.currentPortfolioOrSuperAdmin ==
                              true)) {
                        return Container(
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
                        ));
                      } else {
                        return SizedBox.shrink();
                      }
                    }),
              ],
            ),
            FHPageDivider(),
            SizedBox(height: 8.0),
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
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Card(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        elevation: 5.0,
        child: InkWell(
          borderRadius: BorderRadius.circular(8.0),
          onTap: () {
            bloc.setApplicationId(application.id); //is it the right function?
            return {
              ManagementRepositoryClientBloc.router.navigateTo(
                context,
                '/feature-status',
              )
            };
          },
          child: Container(
            color: Theme.of(context).backgroundColor,
            width: 240,
            height: 150,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        constraints: BoxConstraints(maxWidth: 150),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(application.name,
                                maxLines: 2,
                                style: Theme
                                    .of(context)
                                    .textTheme
                                    .bodyText2
                                    .copyWith(
                                    color: Theme
                                        .of(context)
                                        .primaryColor)),
                            SizedBox(height: 4.0),
                            Text(application.description,
                                maxLines: 2,
//                              overflow: TextOverflow.ellipsis,
                                style: Theme
                                    .of(context)
                                    .textTheme
                                    .caption),
                          ],
                        ),
                      ),
                      StreamBuilder<ReleasedPortfolio>(
                          stream: bloc.mrClient.personState
                              .isCurrentPortfolioOrSuperAdmin,
                          builder: (context, snapshot) {
                            if (snapshot.data != null &&
                                (snapshot.data.currentPortfolioOrSuperAdmin ==
                                    true)) {
                              return _AdminActions(
                                bloc: bloc,
                                application: application,
                              );
                            } else {
                              return SizedBox();
                            }
                          })
                    ],
                  ),
                  Column(
                    children: [
//                      FHPageDivider(),
                      SizedBox(height: 4.0),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          if (application.environments.length
                              .toString()
                              .isNotEmpty)
                            _NumberAndIcon(
                              tooltipText: 'Environments',
                              text: application.environments.length.toString(),
                              icon: Icon(AntDesign.bars,
                                  size: 16.0, color: Colors.deepPurpleAccent),
                            ),
                          if (application.features
                              .where((element) =>
                          element.valueType == FeatureValueType.BOOLEAN)
                              .toList()
                              .isNotEmpty)
                            _NumberAndIcon(
                              tooltipText: 'Feature flags',
                              text: application.features
                                  .where((element) =>
                              element.valueType ==
                                  FeatureValueType.BOOLEAN)
                                  .toList()
                                  .length
                                  .toString(),
                              icon: Icon(Icons.flag,
                                  size: 16.0, color: Colors.green),
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
                            _NumberAndIcon(
                              tooltipText: 'Feature values',
                              icon: Icon(Icons.code,
                                  size: 16.0, color: Colors.blue),
                              text: (((application.features
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
                            ),
                          if (application.features
                              .where((element) =>
                          element.valueType == FeatureValueType.JSON)
                              .toList()
                              .isNotEmpty)
                            _NumberAndIcon(
                              text: application.features
                                  .where((element) =>
                              element.valueType ==
                                  FeatureValueType.JSON)
                                  .toList()
                                  .length
                                  .toString(),
                              tooltipText: 'Configurations',
                              icon: Icon(Icons.device_hub,
                                  size: 16.0, color: Colors.orange),
                            ),
                        ],
                      ),
                    ],
                  )
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _NumberAndIcon extends StatelessWidget {
  final String tooltipText;
  final String text;
  final Icon icon;

  const _NumberAndIcon({
    Key key,
    this.text,
    this.tooltipText,
    this.icon,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltipText,
      child: Column(
        children: [
          _NumberContainer(
            child: Text(text),
          ),
          SizedBox(height: 2.0),
          icon,
        ],
      ),
    );
  }
}

class _AdminActions extends StatelessWidget {
  final ManageAppBloc bloc;
  final Application application;

  const _AdminActions(
      {Key key, @required this.bloc, @required this.application})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(left: 10),
      child: Row(children: <Widget>[
        FHIconButton(
            width: 30.0,
            icon: Icon(Icons.edit,
                color: Theme
                    .of(context)
                    .buttonColor, size: 16.0),
            onPressed: () =>
                bloc.mrClient
                    .addOverlay((BuildContext context) =>
                    AppUpdateDialogWidget(
                      bloc: bloc,
                      application: application,
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
                    application: application,
                  );
                }))
      ]),
    );
  }
}

class _NumberContainer extends StatelessWidget {
  final Widget child;

  const _NumberContainer({Key key, this.child}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        padding: EdgeInsets.symmetric(vertical: 4.0, horizontal: 16.0),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(12.0)),
          color: Colors.white,
        ),
        child: child);
  }
}
