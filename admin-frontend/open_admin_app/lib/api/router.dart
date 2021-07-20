import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/config/route_names.dart';

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
  nowhere,
  login,
  setup
}

// we need to track what states we can be in, what permission types routes have to have, and what the initial route you should have when you are in i

enum RouteSlot { loading, setup, login, personal, portfolio, nowhere }

class RouteSlotMapping {
  final RouteSlot routePermission;
  final List<PermissionType> acceptablePermissionTypes;
  final String initialRoute;

  RouteSlotMapping(
      {required this.routePermission,
      required this.acceptablePermissionTypes,
      required this.initialRoute});
}

Map<RouteSlot, RouteSlotMapping> routeSlotMappings = {};

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
      route = '/feature-dashboard';
    }
    final f = handlers[route];

    return (f == null) ? notFoundHandler.handlerFunc : f.handler.handlerFunc;
  }

  // we don't want to store this as it may change, so always ask the route
  PermissionType permissionForRoute(String route) {
    return handlers[route]?.permissionType ?? PermissionType.regular;
  }

  RouterRoute forNamedRoute(String requestedRouteName, RouteSlot slot) {
    if (ManagementRepositoryClientBloc.router.handlers
        .containsKey(requestedRouteName)) {
      final route =
          ManagementRepositoryClientBloc.router.handlers[requestedRouteName]!;
      final slotPerm = routeSlotMappings[slot]!;

      if (slotPerm.acceptablePermissionTypes.contains(route.permissionType)) {
        return route;
      }

      return ManagementRepositoryClientBloc
          .router.handlers[slotPerm.initialRoute]!;
    }

    // otherwise we are using the 404 route
    return ManagementRepositoryClientBloc.router.handlers['/404']!;
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
    return RouteChange(routeNameFeatureDashboard, params: {});
  }

  bool canUseRoute(String routeName) {
    final rc = RouteChange(routeName);
    print(
        "canUseRoute $routeName super-admin ${mrBloc.userIsSuperAdmin} current portfolio admin ${mrBloc.userIsCurrentPortfolioAdmin} loggedin ${mrBloc.isLoggedIn}");
    return hasRoutePermissions(rc, mrBloc.userIsSuperAdmin,
        mrBloc.userIsCurrentPortfolioAdmin, mrBloc.isLoggedIn);
  }

  bool routeExists(String routeName) {
    return routeName != '/404' && handlers[routeName] != null;
  }
}
