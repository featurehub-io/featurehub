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

Router router = Router();

class Router {
  Handler notFoundHandler;
  ManagementRepositoryClientBloc mrBloc;
  Map<String, Handler> handlers = {};

  void define(String route, {Handler handler, transitionType}) {
    handlers[route] = handler;
  }

  HandlerFunc getRoute(String route) {
    final f = handlers[route];

    return (f == null) ? notFoundHandler : f.handlerFunc;
  }

  void navigateTo(BuildContext context, String route,
      {bool replace,
        TransitionType transition,
        Map<String, List<String>> params}) {
    mrBloc.swapRoutes(RouteChange()
      ..route = route
      ..params = params ?? {}
      ..transition = transition);
  }
}
