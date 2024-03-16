import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/config/route_names.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/fh_label_container.dart';
import 'package:open_admin_app/widgets/common/fh_portfolio_selector.dart';

class DrawerViewWidget extends StatefulWidget {
  const DrawerViewWidget({Key? key}) : super(key: key);

  @override
  DrawerViewWidgetState createState() => DrawerViewWidgetState();
}

class DrawerViewWidgetState extends State<DrawerViewWidget> {
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
                  return StreamBuilder<String?>(
                      stream: mrBloc.streamValley.globalRefresherStream,
                      builder: (context, snapshot) {
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const PortfolioSelectorWidget(),
                            const SizedBox(height: 16),
                            _MenuFeaturesOptionsWidget(mrBloc),
                            StreamBuilder<ReleasedPortfolio?>(
                                stream:
                                    mrBloc.streamValley.currentPortfolioStream,
                                builder: (context, snapshot) {
                                  // print("new released portfolio ${snapshot.data}");
                                  if (!snapshot.hasData ||
                                      !snapshot
                                          .data!.currentPortfolioOrSuperAdmin) {
                                    return const SizedBox.shrink();
                                  }
                                  return Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Padding(
                                        padding: const EdgeInsets.only(
                                            left: 16.0, top: 32.0, bottom: 8.0),
                                        child: Text(
                                          'Application Settings',
                                          style: Theme.of(context)
                                              .textTheme
                                              .bodySmall,
                                        ),
                                      ),
                                      _ApplicationSettings(),
                                      Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: <Widget>[
                                          Padding(
                                            padding: const EdgeInsets.only(
                                                left: 16.0,
                                                top: 32.0,
                                                bottom: 8.0),
                                            child: Text(
                                              'Portfolio Settings',
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodySmall,
                                            ),
                                          ),
                                          _MenuPortfolioAdminOptionsWidget(),
                                          _MenuDivider(),
                                        ],
                                      ),
                                    ],
                                  );
                                }),
                            if (widgetCreator
                                .canSeeOrganisationMenuDrawer(mrBloc))
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: <Widget>[
                                  Container(
                                    padding: const EdgeInsets.only(
                                        left: 16.0, top: 32.0, bottom: 8.0),
                                    child: Text(
                                      'Organization Settings',
                                      style:
                                          Theme.of(context).textTheme.bodySmall,
                                    ),
                                  ),
                                  _SiteAdminOptionsWidget(),
                                  _MenuDivider(),
                                ],
                              )
                          ],
                        );
                      });
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
                iconData: Icons.cases_sharp,
                path: '/portfolios',
                permissionType: PermissionType.portfolioadmin,
                params: {}),
            const FHMenuItem(
                name: 'Users',
                permissionType: PermissionType.portfolioadmin,
                iconData: Icons.group_add,
                path: '/users',
                params: {}),
            const FHMenuItem(
                name: 'Admin Service Accounts',
                permissionType: PermissionType.portfolioadmin,
                iconData: Icons.api_outlined,
                path: '/admin-service-accounts',
                params: {}),
            if (client.identityProviders.systemConfigEnabled)
              const FHMenuItem(
                  name: 'System Config',
                  permissionType: PermissionType.superadmin,
                  iconData: Icons.bike_scooter_rounded,
                  path: '/system-config',
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
            return const Column(children: <Widget>[
              FHMenuItem(
                  name: 'Groups',
                  iconData: Icons.people_outline,
                  path: '/groups',
                  permissionType: PermissionType.portfolioadmin,
                  params: {}),
              FHMenuItem(
                  name: 'Service Accounts',
                  iconData: Icons.build_outlined,
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
        stream: mrClient.streamValley.currentPortfolioIdStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(children: <Widget>[
              const FHMenuItem(
                  name: 'Environments',
                  iconData: Icons.list,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['environments']
                  }),
              const FHMenuItem(
                  name: 'Group permissions',
                  iconData: Icons.groups_2_outlined,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['group-permissions']
                  }),
              const FHMenuItem(
                  name: 'Service account permissions',
                  iconData: Icons.checklist_outlined,
                  path: '/app-settings',
                  permissionType: PermissionType.portfolioadmin,
                  params: {
                    'tab': ['service-accounts']
                  }),
              if (mrClient.identityProviders.capabilityWebhooks)
                const FHMenuItem(
                    name: 'Integrations',
                    iconData: Icons.webhook_outlined,
                    path: '/app-settings',
                    permissionType: PermissionType.portfolioadmin,
                    params: {
                      'tab': ['integrations']
                    }),
            ]);
          } else {
            return const SizedBox.shrink();
          }
        });
  }
}

class _MenuFeaturesOptionsWidget extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const _MenuFeaturesOptionsWidget(this.mrBloc, {Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const FHMenuItem(
          name: 'Applications',
          iconData: Icons.apps_outlined,
          path: '/applications',
          params: {},
        ),
        const FHMenuItem(
          name: 'Features',
          iconData: Icons.flag_outlined,
          path: routeNameFeatureDashboard,
          params: {},
        ),
        if (mrBloc.identityProviders.featureGroupsEnabled)
          const FHMenuItem(
            name: 'Feature Groups',
            iconData: Icons.settings_suggest_sharp,
            path: 'feature-groups',
            params: {},
            displayNewLabel: true,
          ),
        const FHMenuItem(
          name: 'API Keys',
          iconData: Icons.key_outlined,
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
  final bool displayNewLabel;

  const FHMenuItem(
      {Key? key,
      required this.name,
      required this.iconData,
      required this.path,
      required this.params,
      this.permissionType = PermissionType.regular,
      this.iconSize,
      this.displayNewLabel = false})
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

    if (!menuOkForThisUser) {
      return const SizedBox.shrink();
    }

    return InkWell(
      canRequestFocus: false,
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
                  ? Theme.of(context)
                      .colorScheme
                      .primaryContainer
                      .withOpacity(0.6)
                  : null,
              child: Row(
                children: <Widget>[
                  Icon(
                    iconData,
                    size: iconSize ?? 18.0,
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 12.0),
                    child: Text(' $name',
                        style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                              fontWeight: selected
                                  ? FontWeight.bold
                                  : FontWeight.normal,
                            )),
                  ),
                  if (displayNewLabel)
                    const FHLabelContainer(
                      text: "NEW",
                      color: Color(0xff2DCD7A),
                      margin: EdgeInsets.symmetric(horizontal: 8.0),
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
