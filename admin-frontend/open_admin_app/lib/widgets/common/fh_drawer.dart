import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/config/route_names.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/fh_portfolio_selector.dart';

class DrawerViewWidget extends StatefulWidget {
  const DrawerViewWidget({Key? key}) : super(key: key);

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
            return const SizedBox.shrink();
          }
        });
  }
}

class _MenuContainer extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;
  final ScrollController controller = ScrollController();

  _MenuContainer({Key? key, required this.mrBloc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 260,
      height: MediaQuery.of(context).size.height - kToolbarHeight,
      child: Drawer(
        child: ScrollConfiguration(
          behavior: CustomScrollBehavior(),
          child: SingleChildScrollView(
            physics: const ClampingScrollPhysics(),
            controller: controller,
            child: StreamBuilder<Person>(
                stream: mrBloc.personState.personStream,
                builder: (context, snapshot) {
                  if (!snapshot.hasData || !mrBloc.personState.isLoggedIn) {
                    return const SizedBox.shrink();
                  }

                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const PortfolioSelectorWidget(),
                      const SizedBox(height: 16),
                      const _MenuFeaturesOptionsWidget(),
                      StreamBuilder<ReleasedPortfolio?>(
                          stream: mrBloc.streamValley.currentPortfolioStream,
                          builder: (context, snapshot) {
                            // print("new released portfolio ${snapshot.data}");
                            if (!snapshot.hasData ||
                                !snapshot.data!.currentPortfolioOrSuperAdmin) {
                              return const SizedBox.shrink();
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
                      if (mrBloc.userIsSuperAdmin)
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Container(
                              padding: const EdgeInsets.only(
                                  left: 16.0, top: 32.0, bottom: 8.0),
                              child: Text(
                                'Organization Settings',
                                style: Theme.of(context).textTheme.caption,
                              ),
                            ),
                            _SiteAdminOptionsWidget(),
                            _MenuDivider(),
                          ],
                        )
                    ],
                  );
                }),
          ),
        ),
      ),
    );
  }
}

class _SiteAdminOptionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final client = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<String?>(
        stream: client.streamValley.currentPortfolioIdStream,
        builder: (context, snapshot) {
          List<Widget> menus = [
            const FHMenuItem(
                name: 'Portfolios',
                iconData: MaterialCommunityIcons.briefcase_plus_outline,
                path: '/portfolios',
                permissionType: PermissionType.portfolioadmin,
                params: {}),
            const FHMenuItem(
                name: 'Users',
                permissionType: PermissionType.portfolioadmin,
                iconData: AntDesign.addusergroup,
                path: '/users',
                params: {}),
            const FHMenuItem(
                name: 'Admin Service Accounts',
                permissionType: PermissionType.portfolioadmin,
                iconData: AntDesign.API,
                path: '/admin-service-accounts',
                params: {}),
          ];
          menus.addAll(widgetCreator.extraGlobalMenuItems(client));
          return Column(children: menus);
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
            return Column(children: const <Widget>[
              FHMenuItem(
                  name: 'Groups',
                  iconData: MaterialIcons.people_outline,
                  path: '/groups',
                  permissionType: PermissionType.portfolioadmin,
                  params: {}),
              FHMenuItem(
                  name: 'Service Accounts',
                  iconData: AntDesign.tool,
                  permissionType: PermissionType.portfolioadmin,
                  path: '/service-accounts',
                  params: {}),
            ]);
          } else {
            return const SizedBox.shrink();
          }
        });
  }
}

class _ApplicationSettings extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var mrClient = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<String?>(
        stream: mrClient
            .streamValley
            .currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              FHMenuItem(
                  name: 'Environments',
                  iconData: AntDesign.bars,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['environments']
                  }),
              FHMenuItem(
                  name: 'Group permissions',
                  iconData: MaterialCommunityIcons.check_box_multiple_outline,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['group-permissions']
                  }),
              FHMenuItem(
                  name: 'Service account permissions',
                  iconData: MaterialCommunityIcons.cogs,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['service-accounts']
                  }),
              if (mrClient.identityProviders.capabilityWebhooks)
                FHMenuItem(
                    name: 'Webhooks',
                    iconData: MaterialCommunityIcons.cogs,
                    path: '/app-settings',
                    permissionType: PermissionType.portfolioadmin,
                    params: {
                      'tab': ['webhooks']
                    }),
            ]);
          } else {
            return const SizedBox.shrink();
          }
        });
  }
}

class _MenuFeaturesOptionsWidget extends StatelessWidget {
  const _MenuFeaturesOptionsWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: const [
        FHMenuItem(
          name: 'Applications',
          iconData: Feather.grid,
          path: '/applications',
          params: {},
        ),
        FHMenuItem(
          name: 'Features',
          iconData: Feather.flag,
          path: routeNameFeatureDashboard,
          params: {},
        ),
        FHMenuItem(
          name: 'API Keys',
          iconData: AntDesign.key,
          path: '/api-keys',
          params: {},
        )
      ],
    );
  }
}

class FHMenuItem extends StatelessWidget {
  final String name;
  final IconData iconData;
  final double? iconSize;
  final String path;
  final Map<String, List<String>> params;
  final PermissionType permissionType;

  const FHMenuItem(
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
    final menuOkForThisUser =
        ManagementRepositoryClientBloc.router.canUseRoute(path);

    var light = Theme.of(context).brightness == Brightness.light;

    if (!menuOkForThisUser) {
      return const SizedBox.shrink();
    }

    return InkWell(
      canRequestFocus: false,
      mouseCursor: SystemMouseCursors.click,
      hoverColor: light
          ? Theme.of(context).selectedRowColor
          : Theme.of(context).colorScheme.secondary.withOpacity(0.2),
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
            if (!snapshot.hasData) {
              return const SizedBox.shrink();
            }

            final selected = snapshot.data!.route == path &&
                equalsParams(snapshot.data!.params);
            return Container(
              padding: const EdgeInsets.fromLTRB(16, 12, 0, 12),
              color: selected
                  ? (light
                      ? Theme.of(context).primaryColorLight
                      : Theme.of(context).colorScheme.secondary)
                  : null,
              child: Row(
                children: <Widget>[
                  Icon(
                    iconData,
                    size: iconSize ?? 18.0,
                  ),
                  Padding(
                    padding:
                        const EdgeInsets.only(left: 12.0),
                    child: Text(' $name',
                        style: Theme.of(context).textTheme.bodyText2!.copyWith(
                            fontWeight:
                                selected ? FontWeight.bold : FontWeight.normal,
                            color: light
                                ? null
                                : (selected
                                    ? Theme.of(context).primaryColor
                                    : null))),
                  )
                ],
              ),
            );
          }),
    );
  }
}

class _MenuDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        padding: const EdgeInsets.only(top: 16.0),
        decoration: const BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.black, width: 0.5))));
  }
}
