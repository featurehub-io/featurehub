import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/widgets.dart';

typedef HandlerFunc(BuildContext context, Map<String, List<String>> params);

class RouteChange {
  Map<String, List<String>> params;
  TransitionType transition;
  String route;
}

class Handler {
  HandlerFunc handlerFunc;

  Handler({this.handlerFunc}) : assert(handlerFunc != null);
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
    Handler f = handlers[route];

    return (f == null) ? notFoundHandler : f.handlerFunc;
  }

  navigateTo(BuildContext context, String route,
      {bool replace,
      TransitionType transition,
      Map<String, List<String>> params}) {
    mrBloc.swapRoutes(RouteChange()
      ..route = route
      ..params = params ?? {}
      ..transition = transition);
  }
}
