import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/fh_portfolio_selector.dart';

class DrawerViewWidget extends StatefulWidget {
  @override
  _DrawerViewWidgetState createState() => _DrawerViewWidgetState();
}

class _DrawerViewWidgetState extends State<DrawerViewWidget> {
  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    if (MediaQuery.of(context).size.width < 600) {
      mrBloc.menuOpened.add(false);
    } else {
      mrBloc.menuOpened.add(true);
    }

    return StreamBuilder<bool>(
        stream: mrBloc.menuOpened,
        initialData: true,
        builder: (context, snapshot) {
          if (snapshot.data!) {
            return _MenuContainer(
              mrBloc: mrBloc,
            );
          } else {
            return SizedBox.shrink();
          }
        });
  }
}

class _MenuContainer extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const _MenuContainer({Key? key, required this.mrBloc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 260,
      height: MediaQuery.of(context).size.height - kToolbarHeight,
      child: Drawer(
        child: SingleChildScrollView(
          child: StreamBuilder<Person>(
              stream: mrBloc.personState.personStream,
              builder: (context, snapshot) {
                if (!snapshot.hasData || !mrBloc.personState.isLoggedIn) {
                  return SizedBox.shrink();
                }

                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    PortfolioSelectorWidget(),
                    SizedBox(height: 16),
                    _MenuFeaturesOptionsWidget(),
                    StreamBuilder<ReleasedPortfolio?>(
                        stream:
                            mrBloc.personState.isCurrentPortfolioOrSuperAdmin,
                        builder: (context, snapshot) {
                          if (snapshot.data == null ||
                              !snapshot.data!.currentPortfolioOrSuperAdmin) {
                            return SizedBox.shrink();
                          }

                          return Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Padding(
                                padding: const EdgeInsets.only(
                                    left: 16.0, top: 32.0, bottom: 8.0),
                                child: Text(
                                  'Application Settings',
                                  style: Theme.of(context).textTheme.caption,
                                ),
                              ),
                              _ApplicationSettings(),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: <Widget>[
                                  Padding(
                                    padding: const EdgeInsets.only(
                                        left: 16.0, top: 32.0, bottom: 8.0),
                                    child: Text(
                                      'Portfolio Settings',
                                      style:
                                          Theme.of(context).textTheme.caption,
                                    ),
                                  ),
                                  _MenuPortfolioAdminOptionsWidget(),
                                  _MenuDivider(),
                                ],
                              ),
                            ],
                          );
                        }),
                    mrBloc.userIsSuperAdmin
                        ? Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Container(
                                padding: EdgeInsets.only(
                                    left: 16.0, top: 32.0, bottom: 8.0),
                                child: Text(
                                  'Global Settings',
                                  style: Theme.of(context).textTheme.caption,
                                ),
                              ),
                              _SiteAdminOptionsWidget(),
                              _MenuDivider(),
                            ],
                          )
                        : Container(),
                  ],
                );
              }),
        ),
      ),
    );
  }
}

class _SiteAdminOptionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          return Column(children: <Widget>[
            _MenuItem(
                name: 'Portfolios',
                iconData: MaterialCommunityIcons.briefcase_plus_outline,
                path: '/portfolios',
                permissionType: PermissionType.portfolioadmin,
                params: {}),
            _MenuItem(
                name: 'Users',
                permissionType: PermissionType.portfolioadmin,
                iconData: AntDesign.addusergroup,
                path: '/manage-users',
                params: {}),
          ]);
        });
  }
}

class _MenuPortfolioAdminOptionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              _MenuItem(
                  name: 'Groups',
                  iconData: MaterialIcons.people_outline,
                  path: '/manage-group',
                  permissionType: PermissionType.portfolioadmin,
                  params: {}),
              _MenuItem(
                  name: 'Service Accounts',
                  iconData: AntDesign.tool,
                  permissionType: PermissionType.portfolioadmin,
                  path: '/manage-service-accounts',
                  params: {}),
            ]);
          } else {
            return SizedBox.shrink();
          }
        });
  }
}

class _ApplicationSettings extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String?>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              _MenuItem(
                  name: 'Environments',
                  iconData: AntDesign.bars,
                  path: '/manage-app',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab-name': ['environments']
                  }),
              _MenuItem(
                  name: 'Group permissions',
                  iconData: MaterialCommunityIcons.check_box_multiple_outline,
                  path: '/manage-app',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab-name': ['group-permissions']
                  }),
              _MenuItem(
                  name: 'Service account permissions',
                  iconData: MaterialCommunityIcons.cogs,
                  path: '/manage-app',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab-name': ['service-accounts']
                  }),
            ]);
          } else {
            return SizedBox.shrink();
          }
        });
  }
}

class _MenuFeaturesOptionsWidget extends StatelessWidget {
  const _MenuFeaturesOptionsWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _MenuItem(
          name: 'Applications',
          iconData: Feather.grid,
          iconSize: 24,
          path: '/applications',
          params: {},
        ),
        _MenuItem(
          name: 'Features',
          iconData: Feather.flag,
          iconSize: 24,
          path: '/feature-status',
          params: {},
        ),
        _MenuItem(
          name: 'API Keys',
          iconData: AntDesign.key,
          iconSize: 24,
          path: '/service-envs',
          params: {},
        )
      ],
    );
  }
}

class _MenuItem extends StatelessWidget {
  final String name;
  final IconData iconData;
  final iconSize;
  final String path;
  final Map<String, List<String>> params;
  final PermissionType permissionType;

  const _MenuItem(
      {Key? key,
      required this.name,
      required this.iconData,
      required this.path,
      required this.params,
      this.permissionType = PermissionType.regular,
      this.iconSize})
      : super(key: key);

  bool equalsParams(Map<String, List<String>> snapParams) {
    final p1 = snapParams;
    final p2 = params;

    // the p2 keys are the keys _we_ care about, the other keys in the list like
    // 'id' or 'service-name' can be there and don't impact the match.
    return p2.keys
        .every((p2Key) => const ListEquality().equals(p1[p2Key], p2[p2Key]));
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    final menuOkForThisUser = (bloc.userIsCurrentPortfolioAdmin ||
        permissionType == PermissionType.regular);
    var light = Theme.of(context).brightness == Brightness.light;
    return InkWell(
      canRequestFocus: false,
      mouseCursor: SystemMouseCursors.click,
      hoverColor: light
          ? Theme.of(context).selectedRowColor
          : Theme.of(context).accentColor.withOpacity(0.2),
      onTap: () {
        if (menuOkForThisUser) {
          ManagementRepositoryClientBloc.router
              .navigateTo(context, path, params: params);
        }
      },
      child: StreamBuilder<RouteChange?>(
          stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
              .routeCurrentStream,
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              final selected = snapshot.data!.route == path &&
                  equalsParams(snapshot.data!.params);
              return Container(
                padding: EdgeInsets.fromLTRB(16, 12, 0, 12),
                color: selected
                    ? (light
                        ? Theme.of(context).primaryColorLight
                        : Theme.of(context).accentColor)
                    : null,
                child: Row(
                  children: <Widget>[
                    Icon(
                      iconData,
                      color: Theme.of(context).buttonColor,
                      size: iconSize ?? 20.0,
                    ),
                    Padding(
                      padding:
                          EdgeInsets.only(left: iconSize != null ? 18.0 : 24.0),
                      child: Text(' $name',
                          style: Theme.of(context)
                              .textTheme
                              .bodyText2!
                              .copyWith(
                                  fontWeight: selected
                                      ? FontWeight.bold
                                      : FontWeight.normal,
                                  color: light
                                      ? null
                                      : (selected
                                          ? Theme.of(context).primaryColor
                                          : null))),
                    )
                  ],
                ),
              );
            } else {
              return SizedBox.shrink();
            }
          }),
    );
  }
}

class _MenuDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        padding: EdgeInsets.only(top: 16.0),
        decoration: BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.black, width: 0.5))));
  }
}
