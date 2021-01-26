import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/widgets/apps/app_delete_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/app_update_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/apps_bloc.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

class AppsRoute extends StatefulWidget {
  @override
  _AppsRouteState createState() => _AppsRouteState();
}

class _AppsRouteState extends State<AppsRoute> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<AppsBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Wrap(
              children: [
                Container(
                  padding: EdgeInsets.only(bottom: 10),
                  child: FHHeader(
                    title: 'Applications',
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
  final AppsBloc bloc;

  const _ApplicationsCardsList({Key key, @required this.bloc})
      : assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<List<Application>>(
        stream: bloc.currentApplicationsStream,
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
  final AppsBloc bloc;

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
          mouseCursor: SystemMouseCursors.click,
          borderRadius: BorderRadius.circular(8.0),
          onTap: () {
            bloc.mrClient
                .setCurrentAid(application.id); //is it the right function?
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
              padding: const EdgeInsets.only(left: 16.0, bottom: 16, top: 8.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Container(
                        constraints: BoxConstraints(maxWidth: 150),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.start,
                          children: [
                            Text(application.name,
                                maxLines: 2,
                                style: Theme.of(context)
                                    .textTheme
                                    .bodyText2
                                    .copyWith(
                                        color: Theme.of(context).brightness == Brightness.light ? Theme.of(context).primaryColor : null)),
                            SizedBox(height: 4.0),
                            Text(application.description,
                                maxLines: 2,
//                              overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.caption),
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
                              return _PopUpAdminMenu(
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
                      SizedBox(height: 4.0),
                      _AppTotals(application: application),
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

class _AppTotals extends StatelessWidget {
  const _AppTotals({
    Key key,
    @required this.application,
  }) : super(key: key);

  final Application application;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 16.0),
      child: Row(
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
              .where((element) => element.valueType == FeatureValueType.BOOLEAN)
              .toList()
              .isNotEmpty)
            _NumberAndIcon(
              tooltipText: 'Feature flags',
              text: application.features
                  .where(
                      (element) =>
                  element.valueType == FeatureValueType.BOOLEAN)
                  .toList()
                  .length
                  .toString(),
              icon: Icon(Icons.flag, size: 16.0, color: Colors.green),
            ),
          if ((application.features
              .where(
                  (element) => element.valueType == FeatureValueType.STRING)
              .toList()
              .isNotEmpty) ||
              (application.features
                  .where(
                      (element) => element.valueType == FeatureValueType.NUMBER)
                  .toList()
                  .isNotEmpty))
            _NumberAndIcon(
              tooltipText: 'Feature values',
              icon: Icon(Icons.code, size: 16.0, color: Colors.blue),
              text: (((application.features
                  .where((element) =>
              element.valueType == FeatureValueType.STRING)
                  .toList()
                  .length) +
                  (application.features
                      .where((element) =>
                  element.valueType == FeatureValueType.NUMBER)
                      .toList()
                      .length))
                  .toString()),
            ),
          if (application.features
              .where((element) => element.valueType == FeatureValueType.JSON)
              .toList()
              .isNotEmpty)
            _NumberAndIcon(
              text: application.features
                  .where((element) =>
              element.valueType == FeatureValueType.JSON)
                  .toList()
                  .length
                  .toString(),
              tooltipText: 'Configurations',
              icon: Icon(Icons.device_hub, size: 16.0, color: Colors.orange),
            ),
        ],
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

class _NumberContainer extends StatelessWidget {
  final Widget child;

  const _NumberContainer({Key key, this.child}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        padding: EdgeInsets.symmetric(vertical: 4.0, horizontal: 16.0),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(12.0)),
          color: Theme.of(context).cardColor,
        ),
        child: child);
  }
}

class _PopUpAdminMenu extends StatelessWidget {
  final AppsBloc bloc;
  final Application application;

  const _PopUpAdminMenu({Key key, this.bloc, this.application})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
//      width: 34,
//      height: 0,
      child: Material(
        shape: CircleBorder(),
        color: Colors.transparent,
        child: PopupMenuButton(
          tooltip: 'Show more',
          icon: Icon(
            Icons.more_vert,
            size: 22.0,
          ),
          onSelected: (value) {
            if (value == 'edit') {
              bloc.mrClient
                  .addOverlay((BuildContext context) =>
                  AppUpdateDialogWidget(
                    bloc: bloc,
                    application: application,
                      ));
            }
            if (value == 'delete') {
              bloc.mrClient.addOverlay((BuildContext context) {
                return AppDeleteDialogWidget(
                  bloc: bloc,
                  application: application,
                );
              });
            }
            if (value == 'features') {
              bloc.mrClient.setCurrentAid(application.id);
              return {
                ManagementRepositoryClientBloc.router.navigateTo(
                  context,
                  '/feature-status',
                )
              };
            }
          },
          itemBuilder: (BuildContext context) {
            return [
              PopupMenuItem(
                  value: 'features',
                  child:
                  Text('Features', style: Theme
                      .of(context)
                      .textTheme
                      .bodyText2)),
              PopupMenuItem(
                  value: 'edit',
                  child:
                  Text('Edit', style: Theme
                      .of(context)
                      .textTheme
                      .bodyText2)),
              PopupMenuItem(
                  value: 'delete',
                  child: Text('Delete',
                      style: Theme
                          .of(context)
                          .textTheme
                          .bodyText2))
            ];
          },
        ),
      ),
    );
  }
}
