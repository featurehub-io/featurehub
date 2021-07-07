import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';

import 'route_handlers.dart';

final _log = Logger('Routes');

class Routes {
  static final List<String> PUBLIC_URLS = ['/forgot-password', '/register-url'];
  static FHRouter configureRoutes(ManagementRepositoryClientBloc mrBloc) {
    final router = FHRouter(
        mrBloc: mrBloc,
        notFoundHandler: Handler(handlerFunc: (context, params) {
          _log.severe('request for route not found');
          final mrBloc =
              BlocProvider.of<ManagementRepositoryClientBloc>(context);
          mrBloc.customError(messageTitle: 'Oops, page not found');
          return Container();
        }));

    // Public routes (public URL's also need ot be added to array above)
    router.define('/forgot-password',
        handler: handleRouteChangeRequest(routeCreator.forgotPassword));
    router.define('/register-url',
        handler: handleRouteChangeRequest(routeCreator.registerUrl));

    // main app routes
    router.define('/', handler: handleRouteChangeRequest(routeCreator.root));
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
