import 'dart:js' as js;

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:app_singleapp/widgets/common/fh_circle_icon_button.dart';
import 'package:app_singleapp/widgets/common/fh_portfolio_selector.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:google_fonts/google_fonts.dart';

import 'fh_nav_rail.dart';

class DrawerViewWidget extends StatefulWidget {
  @override
  _DrawerViewWidgetState createState() => _DrawerViewWidgetState();
}

class _DrawerViewWidgetState extends State<DrawerViewWidget> {
  final int _HEADER_PADDING = 56;

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return StreamBuilder<bool>(
        stream: mrBloc.menuOpened,
        initialData: true,
        builder: (context, snapshot) {
          if (snapshot.data) {
            return _MenuContainer(
              mrBloc: mrBloc,
              headerPadding: _HEADER_PADDING,
            );
          } else {
            return NavRail(
              mrBloc: mrBloc,
            );
          }
        });
  }
}

class _MenuContainer extends StatelessWidget {
  final int headerPadding;
  final ManagementRepositoryClientBloc mrBloc;

  const _MenuContainer({Key key, this.headerPadding, this.mrBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 260,
      height: MediaQuery.of(context).size.height - headerPadding,
      child: Drawer(
        child: SingleChildScrollView(
          child: Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Padding(
              padding: const EdgeInsets.all(4.0),
              child: CircleIconButton(
                  icon: Icon(Icons.chevron_left),
                  onTap: () => mrBloc.menuOpened.add(false)),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                PortfolioSelectorWidget(),
                SizedBox(height: 16),
                _MenuFeaturesOptionsWidget(),
                Padding(
                  padding:
                      const EdgeInsets.only(left: 16.0, top: 32.0, bottom: 8.0),
                  child: Text(
                    'Application Settings',
                    style: Theme.of(context).textTheme.caption,
                  ),
                ),
                _ApplicationSettings(),
                mrBloc.userIsAnyPortfolioOrSuperAdmin
                    ? Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Padding(
                            padding: const EdgeInsets.only(
                                left: 16.0, top: 32.0, bottom: 8.0),
                            child: Text(
                              'Portfolio Settings',
                              style: Theme.of(context).textTheme.caption,
                            ),
                          ),
                          _MenuPortfolioAdminOptionsWidget(),
                          _MenuDivider(),
                        ],
                      )
                    : Container(),
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
            )
          ]),
        ),
      ),
    );
  }
}

void _logout(BuildContext context, ManagementRepositoryClientBloc client) {
  client.logout().then((result) {
    // the better way to do this is probably to reload the main app.
    js.context['location']['href'] = '/';
  }).catchError((e, s) => client.dialogError(e, s));
}

Widget _getHomeOptionsWidget(BuildContext context) {
  return Column(children: [
    _MenuItem(
        name: 'Recent updates', iconData: Icons.home, path: '/', params: {}),
    Container(
      padding: EdgeInsets.only(left: 8, bottom: 20),
      child: Divider(
        thickness: 1.0,
      ),
    ),
  ]);
}

class _SiteAdminOptionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          return Column(children: <Widget>[
            _MenuItem(
                name: "Portfolios",
                iconData: Icons.business_center,
                path: "/portfolios",
                params: {}),
            _MenuItem(
                name: "Users",
                iconData: AntDesign.addusergroup,
                path: "/manage-users",
                params: {}),
          ]);
        });
  }
}

class _MenuPortfolioAdminOptionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<String>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              _MenuItem(
                  name: 'Groups',
                  iconData: Icons.group,
                  path: '/manage-group',
                  params: {}),
              _MenuItem(
                  name: 'Service Accounts',
                  iconData: Icons.build,
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
    return StreamBuilder<String>(
        stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              _MenuItem(
                  name: "Environments",
                  iconData: AntDesign.bars,
                  path: "/manage-app",
                  params: {
                    "tab-name": ["environments"]
                  }),
              _MenuItem(
                  name: "Group permissions",
                  iconData: MaterialCommunityIcons.check_box_multiple_outline,
                  path: "/manage-app",
                  params: {
                    "tab-name": ["group-permissions"]
                  }),
              _MenuItem(
                  name: "Service account permissions",
                  iconData: MaterialCommunityIcons.cogs,
                  path: "/manage-app",
                  params: {
                    "tab-name": ["service-accounts"]
                  }),
            ]);
          } else {
            return SizedBox.shrink();
          }
        });
  }
}

class _MenuFeaturesOptionsWidget extends StatelessWidget {
  final RouteChange currentRoute;

  const _MenuFeaturesOptionsWidget({Key key, this.currentRoute})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return _MenuItem(
      name: "Features",
      iconData: Icons.dashboard,
      path: "/feature-status",
      params: {},
    );
  }
}

class _MenuItem extends StatelessWidget {
  final String name;
  final IconData iconData;
  final String path;
  final Map<String, List<String>> params;

  const _MenuItem({Key key, this.name, this.iconData, this.path, this.params})
      : super(key: key);

  bool equalsParams(Map<String, List<String>> snapParams) {
    Map<String, List<String>> p1 = {}
      ..addAll(snapParams ?? {})
      ..remove('id');
    Map<String, List<String>> p2 = {}..addAll(params ?? {});

    return const MapEquality(
            keys: const IdentityEquality(), values: const ListEquality())
        .equals(p1, p2);
  }

  @override
  Widget build(BuildContext context) {
    return CustomCursor(
        child: InkWell(
      hoverColor: Theme.of(context).selectedRowColor,
      onTap: () {
        return ManagementRepositoryClientBloc.router.navigateTo(context, path,
            replace: true, transition: TransitionType.material, params: params);
      },
      child: StreamBuilder<RouteChange>(
          stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
              .currentRoute,
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              final selected = snapshot.data.route == path &&
                  equalsParams(snapshot.data.params);

              return Container(
                padding: EdgeInsets.fromLTRB(16, 12, 0, 12),
                color: selected ? Color(0xffe5e7f1) : null,
                child: Row(
                  children: <Widget>[
                    Icon(
                      iconData,
                      color: selected
                          ? Theme.of(context).primaryColor
                          : Color(0xff4a4a4a),
                      size: 16.0,
                    ),
                    Padding(
                      padding: const EdgeInsets.only(left: 24.0),
                      child: selected
                          ? Text(" ${name}",
                              style: GoogleFonts.roboto(
                                textStyle:
                                    Theme.of(context).textTheme.bodyText2,
                                fontWeight: FontWeight.w600,
                                color: Theme.of(context).primaryColor,
                              ))
                          : Text(" ${name}",
                              style: Theme.of(context).textTheme.bodyText2),
                    )
                  ],
                ),
              );
            } else {
              return SizedBox.shrink();
            }
          }),
    ));
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
