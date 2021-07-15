import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';

import 'route_handlers.dart';

final _log = Logger('Routes');

class Routes {
  static FHRouter configureRoutes(ManagementRepositoryClientBloc mrBloc) {
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
          PermissionType.regular
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
        permissionType: PermissionType.any);
    // Public routes (public URL's also need ot be added to array above)
    router.define('/forgot-password',
        handler: handleRouteChangeRequest(routeCreator.forgotPassword),
        permissionType: PermissionType.login);
    router.define('/register-url',
        handler: handleRouteChangeRequest(routeCreator.registerUrl),
        permissionType: PermissionType.login);
    router.define('/setup',
        handler: handleRouteChangeRequest(routeCreator.setup),
        permissionType: PermissionType.setup);
    router.define('/login',
        handler: handleRouteChangeRequest(routeCreator.login),
        permissionType: PermissionType.login);
    // main app routes
    router.define('/', handler: handleRouteChangeRequest(routeCreator.root));
    router.define('/loading',
        handler: handleRouteChangeRequest(routeCreator.loading),
        permissionType: PermissionType.any);
    router.define('', handler: handleRouteChangeRequest(routeCreator.root));
    router.define('/applications',
        handler: handleRouteChangeRequest(routeCreator.apps));
    router.define('/feature-status',
        handler: handleRouteChangeRequest(routeCreator.featureStatus));
    router.define('/feature-values',
        handler: handleRouteChangeRequest(routeCreator.featureValues));

    router.define('/service-envs',
        handler: handleRouteChangeRequest(routeCreator.serviceEnvsHandler));

    //Admin routes
    router.define('/create-user',
        handler: handleRouteChangeRequest(routeCreator.createUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/portfolios',
        handler: handleRouteChangeRequest(routeCreator.portfolios));
    router.define('/manage-app',
        handler: handleRouteChangeRequest(routeCreator.manageApp),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-group',
        handler: handleRouteChangeRequest(routeCreator.group),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-service-accounts',
        handler: handleRouteChangeRequest(routeCreator.serviceAccount),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-user',
        handler: handleRouteChangeRequest(routeCreator.manageUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-users',
        handler: handleRouteChangeRequest(routeCreator.users),
        permissionType: PermissionType.portfolioadmin);

    return router;
  }
}
