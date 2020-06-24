import 'dart:convert';

import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/widgets.dart';

typedef HandlerFunc = Widget Function(
    BuildContext context, Map<String, List<String>> params);

class RouteChange {
  Map<String, List<String>> params;
  TransitionType transition;
  String route;

  static RouteChange fromJson(String json) {
    final j = jsonDecode(json);
    return RouteChange()
      ..params = Map<String, List<String>>.from(j['params'].map((k, v) =>
          MapEntry<String, List<String>>(k.toString(), List<String>.from(v))))
      ..route = j['route'].toString();
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

  Handler({@required this.handlerFunc}) : assert(handlerFunc != null);
}

enum TransitionType { fadeIn, material }
enum PermissionType { superadmin, portfolioadmin, regular }

Router router = Router();

class RouterRoute {
  Handler handler;
  TransitionType transitionType;
  PermissionType permissionType = PermissionType.regular;
}

class Router {
  Handler notFoundHandler;
  ManagementRepositoryClientBloc mrBloc;
  Map<String, RouterRoute> handlers = {};

  void define(String route,
      {Handler handler,
      TransitionType transitionType = TransitionType.material,
      PermissionType permissionType = PermissionType.regular}) {
    handlers[route] = RouterRoute()
      ..handler = handler
      ..permissionType = permissionType;
  }

  HandlerFunc getRoute(String route) {
    if (route == '/') {
      route = '/feature-status';
    }
    final f = handlers[route];

    return (f == null || f.handler == null)
        ? notFoundHandler.handlerFunc
        : f.handler.handlerFunc;
  }

  // we don't want to store this as it may change, so always ask the route
  PermissionType permissionForRoute(String route) {
    return handlers[route]?.permissionType ?? PermissionType.regular;
  }

  bool hasRoutePermissions(
      RouteChange route, bool superuser, bool portfolioAdmin) {
    if (superuser) {
      return true;
    }

    final perm = permissionForRoute(route.route);

    if (perm == PermissionType.portfolioadmin && portfolioAdmin) {
      return true;
    }

    return perm == PermissionType.regular;
  }

  void navigateTo(BuildContext context, String route,
      {TransitionType transition, Map<String, List<String>> params}) {
    final rc = RouteChange()
      ..route = route
      ..params = params ?? {}
      ..transition = transition;

    if (hasRoutePermissions(
        rc, mrBloc.userIsSuperAdmin, mrBloc.userIsCurrentPortfolioAdmin)) {
      mrBloc.swapRoutes(rc);
    } else {
      mrBloc.swapRoutes(defaultRoute());
    }
  }

  RouteChange defaultRoute() {
    return RouteChange()
      ..route = '/feature-status'
      ..params = {};
  }
}
