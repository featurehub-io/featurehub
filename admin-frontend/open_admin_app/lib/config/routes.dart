import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/config/route_names.dart';

import 'route_handlers.dart';

final _log = Logger('Routes');

class Routes {
  static FHRouter configureRoutes(ManagementRepositoryClientBloc mrBloc) {
    // no particular permission of any kind is required for this, but if you use NONE it is a no-where slot
    routeSlotMappings[RouteSlot.nowhere] = RouteSlotMapping(
        routePermission: RouteSlot.nowhere,
        acceptablePermissionTypes: [PermissionType.none],
        initialRoute: '/404');

    routeSlotMappings[RouteSlot.loading] = RouteSlotMapping(
        routePermission: RouteSlot.loading,
        acceptablePermissionTypes: [PermissionType.any],
        initialRoute: '/loading');

    routeSlotMappings[RouteSlot.login] = RouteSlotMapping(
        routePermission: RouteSlot.login,
        acceptablePermissionTypes: [PermissionType.login, PermissionType.any],
        initialRoute: '/login');

    routeSlotMappings[RouteSlot.setup] = RouteSlotMapping(
        routePermission: RouteSlot.setup,
        acceptablePermissionTypes: [PermissionType.setup, PermissionType.any],
        initialRoute: '/setup');

    routeSlotMappings[RouteSlot.portfolio] = RouteSlotMapping(
        routePermission: RouteSlot.portfolio,
        acceptablePermissionTypes: [
          PermissionType.personal,
          PermissionType.any,
          PermissionType.portfolioadmin,
          PermissionType.superadmin,
          PermissionType.regular,
          PermissionType.extra1,
          PermissionType.extra2,
          PermissionType.extra3,
        ],
        initialRoute: '/applications');

    final router = FHRouter(
        mrBloc: mrBloc,
        notFoundHandler: Handler(handlerFunc: (context, params) {
          _log.severe('request for route not found');
          final mrBloc =
              BlocProvider.of<ManagementRepositoryClientBloc>(context);
          mrBloc.customError(messageTitle: 'Oops, page not found');
          return Container();
        }));

    router.define('/404',
        handler: handleRouteChangeRequest(routeCreator.notFound),
        routeSlots: [RouteSlot.nowhere],
        permissionType: PermissionType.any,
        wrapInScaffold: false);
    // Public routes (public URL's also need ot be added to array above)
    router.define('/forgot-password',
        handler: handleRouteChangeRequest(routeCreator.forgotPassword),
        routeSlots: [RouteSlot.login],
        permissionType: PermissionType.login,
        wrapInScaffold: false);
    router.define('/register-url',
        handler: handleRouteChangeRequest(routeCreator.registerUrl),
        routeSlots: [RouteSlot.login],
        permissionType: PermissionType.login,
        wrapInScaffold: false);
    router.define('/setup',
        handler: handleRouteChangeRequest(routeCreator.setup),
        routeSlots: [RouteSlot.setup],
        permissionType: PermissionType.setup,
        wrapInScaffold: false);
    router.define("/oauth2-failure",
        handler: handleRouteChangeRequest(routeCreator.oauth2Fail),
        routeSlots: [RouteSlot.login],
        permissionType: PermissionType.any,
        wrapInScaffold: false);
    router.define('/login',
        handler: handleRouteChangeRequest(routeCreator.login),
        routeSlots: [RouteSlot.login],
        permissionType: PermissionType.login,
        wrapInScaffold: false);
    // main app routes
    router.define('/',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.root),
        wrapInScaffold: false);
    router.define('',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.root),
        wrapInScaffold: false);
    router.define('/applications',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.apps));
    // NEVER EVER use /features as that is reserved for use by the Edge app
    router.define(routeNameFeatureDashboard,
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.featureStatus));
    router.define('/feature-values',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.featureValues));

    router.define('/api-keys',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.serviceEnvsHandler));

    //Admin routes
    router.define('/create-user',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.createUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/create-admin-api-key',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.createAdminApiKey),
        permissionType: PermissionType.portfolioadmin);
    router.define('/portfolios',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.portfolios));
    router.define('/app-settings',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.manageApp),
        permissionType: PermissionType.portfolioadmin);
    router.define('/groups',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.group),
        permissionType: PermissionType.portfolioadmin);
    router.define('/service-accounts',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.serviceAccount),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-user',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.manageUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/edit-admin-service-account',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.editAdminApiKey),
        permissionType: PermissionType.portfolioadmin);
    router.define('/users',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.users),
        permissionType: PermissionType.portfolioadmin);
    router.define('/admin-service-accounts',
        routeSlots: [RouteSlot.portfolio],
        handler: handleRouteChangeRequest(routeCreator.adminAPIKeys),
        permissionType: PermissionType.portfolioadmin);

    return router;
  }
}
