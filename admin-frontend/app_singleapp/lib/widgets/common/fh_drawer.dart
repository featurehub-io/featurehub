import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/common/stream_valley.dart';
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  PortfolioSelectorWidget(),
                  Padding(
                    padding: const EdgeInsets.only(
                        left: 16.0, top: 16.0, right: 8.0, bottom: 16.0),
                    child: CircleIconButton(
                        icon: Icon(Icons.chevron_left),
                        onTap: () => mrBloc.menuOpened.add(false)),
                  ),
                ],
              ),
              SizedBox(height: 16),
              _MenuFeaturesOptionsWidget(),
              StreamBuilder<ReleasedPortfolio>(
                  stream: mrBloc.personState.isCurrentPortfolioOrSuperAdmin,
                  builder: (context, snapshot) {
                    if (snapshot.data != null &&
                        (snapshot.data.currentPortfolioOrSuperAdmin == true)) {
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
                                  style: Theme.of(context).textTheme.caption,
                                ),
                              ),
                              _MenuPortfolioAdminOptionsWidget(),
                              _MenuDivider(),
                            ],
                          ),
                        ],
                      );
                    } else {
                      return Container();
                    }
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
          ),
        ),
      ),
    );
  }
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
    return StreamBuilder<String>(
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
    return StreamBuilder<String>(
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
  final RouteChange currentRoute;

  const _MenuFeaturesOptionsWidget({Key key, this.currentRoute})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _MenuItem(
          name: 'Applications',
          iconData: Icons.apps,
          iconSize: 26,
          path: '/applications',
          params: {},
        ),
        _MenuItem(
          name: 'Features',
          iconData: Ionicons.md_switch,
          iconSize: 26,
          path: '/feature-status',
          params: {},
        ),
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
      {Key key,
      this.name,
      this.iconData,
      this.path,
      this.params,
      this.permissionType = PermissionType.regular,
      this.iconSize})
      : super(key: key);

  bool equalsParams(Map<String, List<String>> snapParams) {
    final p1 = snapParams ?? {};
    final p2 = params ?? {};

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
    return CustomCursor(
        child: InkWell(
      hoverColor: Theme.of(context).selectedRowColor,
      onTap: () {
        if (menuOkForThisUser) {
          ManagementRepositoryClientBloc.router.navigateTo(context, path,
              transition: TransitionType.material, params: params);
        }
      },
      child: StreamBuilder<RouteChange>(
          stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
              .routeChangedStream,
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
                      size: iconSize ?? 20.0,
                    ),
                    Padding(
                      padding:
                          EdgeInsets.only(left: iconSize != null ? 18.0 : 24.0),
                      child: selected
                          ? Text(' ${name}',
                              style: GoogleFonts.roboto(
                                textStyle:
                                    Theme.of(context).textTheme.bodyText2,
                                fontWeight: menuOkForThisUser
                                    ? FontWeight.w600
                                    : FontWeight.w100,
                                color: menuOkForThisUser
                                    ? Theme.of(context).primaryColor
                                    : Colors.red,
                              ))
                          : Text(
                              ' ${name}',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodyText2
                                  .copyWith(
                                      color: menuOkForThisUser
                                          ? null
                                          : Colors.red),
                            ),
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
