import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:app_singleapp/widgets/common/fh_circle_icon_button.dart';
import 'package:app_singleapp/widgets/common/fh_portfolio_selector.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'dart:js' as js;

import 'fh_nav_rail.dart';

class DrawerViewWidget extends StatefulWidget {
  @override
  _DrawerViewWidgetState createState() => _DrawerViewWidgetState();
}

class _DrawerViewWidgetState extends State<DrawerViewWidget> {
  final int _HEADER_PADDING = 56;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return StreamBuilder<bool>(
        stream: mrBloc.menuOpened,
        initialData: true,
        builder: (context, snapshot) {
          if (snapshot.data) {
            return MenuContainer(
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

class MenuContainer extends StatelessWidget {
  final int headerPadding;
  final ManagementRepositoryClientBloc mrBloc;

  const MenuContainer({Key key, this.headerPadding, this.mrBloc})
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
                _getFeaturesOptionsWidget(context),
                mrBloc.userIsAnyPortfolioOrSuperAdmin
                    ? Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Padding(
                            padding: const EdgeInsets.only(
                                left: 16.0, top: 32.0, bottom: 8.0),
                            child: Text(
                              'Settings',
                              style: Theme.of(context).textTheme.caption,
                            ),
                          ),
                          _getPortfolioAdminOptionsWidget(context),
                          MenuDivider(),
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
                          _getSiteAdminOptionsWidget(context),
                          MenuDivider(),
                        ],
                      )
                    : Container(),
              ],
            ),
          ]),
        ),
      ),
    );
  }
}

void _logout(BuildContext context, ManagementRepositoryClientBloc client) {
  client.logout().then((result) {
    // the better way to do this is probably to reload the main app.
    js.context['location']['href'] = "/";
  }).catchError((e, s) => client.dialogError(e, s));
}

Widget _getHomeOptionsWidget(BuildContext context) {
  return Column(children: [
    menuItem(context, "Recent updates", Icons.home, "/", 2),
    Container(
      padding: EdgeInsets.only(left: 8, bottom: 20),
      child: Divider(
        thickness: 1.0,
      ),
    ),
  ]);
}

Widget _getSiteAdminOptionsWidget(BuildContext context) {
  return StreamBuilder<String>(
      stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
          .streamValley
          .currentPortfolioIdStream,
      builder: (context, snapshot) {
        return Column(children: <Widget>[
          menuItem(
              context, "Portfolios", Icons.business_center, "/portfolios", 2),
          menuItem(context, "Users", Icons.person, "/manage-users", 2),
        ]);
      });
}

Widget _getPortfolioAdminOptionsWidget(BuildContext context) {
  return StreamBuilder<String>(
      stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
          .streamValley
          .currentPortfolioIdStream,
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          return Column(children: <Widget>[
            menuItem(context, "Applications", Icons.web, "/manage-app", 2),
            menuItem(context, "Groups   ", Icons.group, "/manage-group", 2),
            menuItem(context, "Service Accounts", Icons.build,
                "/manage-service-accounts", 2),
          ]);
        } else {
          return SizedBox.shrink();
        }
      });
}

Widget _getFeaturesOptionsWidget(BuildContext context) {
  return menuItem(context, "Features", Icons.dashboard, "/feature-status", 2);
}

Widget menuItem(BuildContext context, String name, IconData iconData,
    String path, double level,
    [bool selected = false]) {
  return CustomCursor(
    child: GestureDetector(
      onTap: () {
        return ManagementRepositoryClientBloc.router.navigateTo(context, path,
            replace: true, transition: TransitionType.material);
      },
      child: highlightContainer(
          Container(
            //padding: EdgeInsets.only(bottom: 18),
            child: Row(
              children: <Widget>[
                Icon(
                  iconData,
                  color: Color(0xff4a4a4a),
                  size: 16.0,
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 24.0),
                  child: Text(" ${name}",
                      style: Theme.of(context).textTheme.bodyText2),
                )
              ],
            ),
          ),
          selected,
          level,
          context),
    ),
  );
}

Widget highlightContainer(
    Widget child, bool selected, double level, BuildContext context) {
  if (!selected) {
    return Container(
      child: child,
      padding: EdgeInsets.fromLTRB(16, 12, 0, 12),
    );
  }
  return Container(
      margin: EdgeInsets.all(0),
      padding: EdgeInsets.fromLTRB(8, 12, 0, 12),
      decoration: BoxDecoration(
          color: Color(0xffe5e7f1),
          borderRadius: BorderRadius.only(
              bottomRight: const Radius.circular(25.0),
              topRight: const Radius.circular(25.0))),
      child: child);
}

class MenuDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        padding: EdgeInsets.only(top: 16.0),
        decoration: BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.black, width: 0.5))));
  }
}
