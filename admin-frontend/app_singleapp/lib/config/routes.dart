import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
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
        handler: handleRouteChangeRequest(forgotPassword));
    router.define('/register-url',
        handler: handleRouteChangeRequest(registerUrl));

    // main app routes
    router.define('/', handler: handleRouteChangeRequest(root));
    router.define('', handler: handleRouteChangeRequest(root));
    router.define('/applications', handler: handleRouteChangeRequest(apps));
    router.define('/feature-status',
        handler: handleRouteChangeRequest(featureStatus));
    router.define('/feature-values',
        handler: handleRouteChangeRequest(featureValues));

    router.define('/service-envs',
        handler: handleRouteChangeRequest(serviceEnvsHandler));

    //Admin routes
    router.define('/create-user',
        handler: handleRouteChangeRequest(createUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/portfolios', handler: handleRouteChangeRequest(portfolios));
    router.define('/manage-app',
        handler: handleRouteChangeRequest(manageApp),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-group',
        handler: handleRouteChangeRequest(group),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-service-accounts',
        handler: handleRouteChangeRequest(serviceAccount),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-user',
        handler: handleRouteChangeRequest(manageUser),
        permissionType: PermissionType.portfolioadmin);
    router.define('/manage-users',
        handler: handleRouteChangeRequest(users),
        permissionType: PermissionType.portfolioadmin);

    return router;
  }
}
