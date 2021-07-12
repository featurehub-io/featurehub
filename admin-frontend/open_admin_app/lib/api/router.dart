import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';

typedef HandlerFunc = Widget Function(
    BuildContext context, Map<String, List<String>> params);

class RouteChange {
  Map<String, List<String>> params;
  String route;

  RouteChange(this.route, {this.params = const {}});

  static RouteChange fromJson(String json) {
    final j = jsonDecode(json);

    return RouteChange(
      j['route'].toString(),
      params: Map<String, List<String>>.from(j['params'].map((k, v) =>
          MapEntry<String, List<String>>(k.toString(), List<String>.from(v)))),
    );
  }

  String toJson() {
    return jsonEncode({'route': route, 'params': params});
  }

  @override
  String toString() {
    return 'route: $route - params $params';
  }
}

class Handler {
  HandlerFunc handlerFunc;

  Handler({required this.handlerFunc});
}

enum TransitionType { fadeIn, material }
enum PermissionType {
  superadmin,
  portfolioadmin,
  regular,
  personal,
  any,
  login,
  setup
}

List<PermissionType> scaffoldWrapPermissions = [
  PermissionType.superadmin,
  PermissionType.portfolioadmin,
  PermissionType.regular
];

class RouterRoute {
  Handler handler;
  PermissionType permissionType;

  RouterRoute(this.handler, {this.permissionType = PermissionType.regular});
}

class FHRouter {
  final Handler notFoundHandler;
  final ManagementRepositoryClientBloc mrBloc;
  final Map<String, RouterRoute> handlers = {};

  FHRouter({required this.mrBloc, required this.notFoundHandler});

  void define(String route,
      {required Handler handler,
      TransitionType transitionType = TransitionType.material,
      PermissionType permissionType = PermissionType.regular}) {
    handlers[route] = RouterRoute(handler, permissionType: permissionType);
  }

  HandlerFunc getRoute(String route) {
    if (route == '/') {
      route = '/feature-status';
    }
    final f = handlers[route];

    return (f == null) ? notFoundHandler.handlerFunc : f.handler.handlerFunc;
  }

  // we don't want to store this as it may change, so always ask the route
  PermissionType permissionForRoute(String route) {
    return handlers[route]?.permissionType ?? PermissionType.regular;
  }

  RouterRoute forNamedRoute(String? requestedRouteName, String defaultRouteName,
      {PermissionType? permissionType}) {
    final routeName = requestedRouteName ?? defaultRouteName;

    final route = ManagementRepositoryClientBloc.router.handlers[routeName] ??
        ManagementRepositoryClientBloc.router.handlers[defaultRouteName]!;
    final defaultRoute =
        ManagementRepositoryClientBloc.router.handlers[defaultRouteName]!;
    final routePerm = route.permissionType;

    if (permissionType == PermissionType.setup && routePerm != permissionType ||
        permissionType == PermissionType.login && routePerm != permissionType) {
      return defaultRoute;
    }

    if (!hasRoutePermissions(
        RouteChange(route == defaultRoute ? defaultRouteName : routeName),
        mrBloc.userIsSuperAdmin,
        mrBloc.userIsCurrentPortfolioAdmin,
        mrBloc.isLoggedIn)) {
      return defaultRoute;
    }

    return route;
  }

  bool hasRoutePermissions(
      RouteChange route, bool superuser, bool portfolioAdmin, bool isLoggedIn) {
    final perm = permissionForRoute(route.route);

    if (perm == PermissionType.any) {
      return true;
    }

    if (perm == PermissionType.login && !isLoggedIn) {
      return true;
    }

    if (perm != PermissionType.login && perm != PermissionType.setup) {
      if (superuser == true) {
        return true;
      }

      if (perm == PermissionType.portfolioadmin && portfolioAdmin == true) {
        return true;
      }

      return perm == PermissionType.regular || perm == PermissionType.personal;
    }

    return false;
  }

  void navigateRoute(String route, {Map<String, List<String>>? params}) {
    final rc = RouteChange(route, params: params ?? const {});

    if (hasRoutePermissions(rc, mrBloc.userIsSuperAdmin,
        mrBloc.userIsCurrentPortfolioAdmin, mrBloc.isLoggedIn)) {
      mrBloc.swapRoutes(rc);
    } else {
      mrBloc.swapRoutes(defaultRoute());
    }
  }

  void navigateTo(BuildContext? context, String route,
      {Map<String, List<String>>? params}) {
    navigateRoute(route, params: params);
  }

  RouteChange defaultRoute() {
    return RouteChange('/feature-status', params: {});
  }
}
