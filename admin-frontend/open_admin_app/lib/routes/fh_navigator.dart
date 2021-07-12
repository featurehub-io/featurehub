import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/widgets/common/fh_scaffold.dart';

final String routeSetupApp = 'setup';
final String routeLoginApp = 'login';

class FHRoutePath {
  String? _routeName;
  Map<String, List<String>>? params;

  set routeName(String? value) => _routeName =
      value == null ? null : (value.startsWith('/') ? value : '/$value');
  String? get routeName => _routeName;

  RouteInformation make() {
    if (_routeName != null) {
      return RouteInformation(location: _routeName ?? '/loading');
    }

    return RouteInformation(location: '404');
  }

  @override
  String toString() {
    return 'FHRoutePath{_routeName: $_routeName, params: $params}';
  }
}

class RouteWrapperWidget extends StatelessWidget {
  final Widget child;
  const RouteWrapperWidget({Key? key, required this.child}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHScaffoldWidget(
        bodyMainAxisAlignment: MainAxisAlignment.center, body: child);
  }
}

class FHRouteDelegate extends RouterDelegate<FHRoutePath>
    with ChangeNotifier, PopNavigatorRouterDelegateMixin<FHRoutePath> {
  final GlobalKey<NavigatorState> _navigatorKey;
  final ManagementRepositoryClientBloc bloc;
  bool isLoggedIn = false;
  bool? isInitialised;
  FHRoutePath? path;

  @override
  FHRoutePath get currentConfiguration {
    if (isInitialised == null) return FHRoutePath()..routeName = '/loading';
    if (isInitialised == false) return FHRoutePath()..routeName = '/setup';
    if (isInitialised == true && isLoggedIn == false) {
      return FHRoutePath()..routeName = '/login';
    }

    return path == null ? (FHRoutePath()..routeName = '/applications') : path!;
  }

  @override
  Widget build(BuildContext context) {
    return Navigator(
      key: navigatorKey,
      pages: [
        if (isInitialised == null)
          routeWrapperPage(context, '/loading', '/loading'),
        if (isInitialised == false)
          routeWrapperPage(context, '/setup', '/404',
              permissionType: PermissionType.setup),
        if (isInitialised == true && isLoggedIn == false)
          routeWrapperPage(context, '/login', '/404',
              permissionType: PermissionType.login),
        if (isInitialised == true && isLoggedIn == true)
          routeWrapperPage(context, path?.routeName ?? '/loading', '/404',
              params: path?.params),
      ],
      onPopPage: (route, result) {
        if (!route.didPop(result)) {
          return false;
        }

        notifyListeners();

        return true;
      },
    );
  }

  MaterialPage routeWrapperPage(
      BuildContext context, String? route, String defaultUrl,
      {PermissionType? permissionType, Map<String, List<String>>? params}) {
    final handler =
        ManagementRepositoryClientBloc.router.forNamedRoute(route, defaultUrl);

    final child = handler.handler.handlerFunc(context, {});
    final wrapWidget = scaffoldWrapPermissions.contains(handler.permissionType);

    return MaterialPage(
        key: ValueKey('app'),
        child: wrapWidget ? RouteWrapperWidget(child: child) : child);
  }

  @override
  FHRouteDelegate(this.bloc) : _navigatorKey = GlobalKey<NavigatorState>() {
    // notifyListeners simply causes the build() method above to be called.

    // TODO: note these listeners have NO cleanup. If a user changes them we will start getting two events

    // listen for changes in  the person and make sure we alter our internal state to match
    // and then force a rebuild. We are just checking if they went from logged in to logged out
    bloc.personState.personStream.listen((p) {
      if (bloc.isLoggedIn != isLoggedIn) {
        isLoggedIn = bloc.isLoggedIn;
        notifyListeners();
      }
    });

    // listen for an internal route change. This is someone calling ManagementRepositoryClientBloc.route.swapRoutes
    // which currently exists all over the code base.
    bloc.routeChangedStream.listen((r) {
      if (r != null) {
        if (isLoggedIn && path?.routeName != r.route) {
          path = FHRoutePath()
            ..routeName = r.route
            ..params = r.params;
          notifyListeners();
        }
      }
    });

    bloc.siteInitialisedSource.listen((s) {
      if (s != isInitialised) {
        isInitialised = s;
        notifyListeners();
      }
    });
  }

  @override
  Future<void> setNewRoutePath(FHRoutePath configuration) async {
    path = configuration;

    if (isInitialised == true && isLoggedIn) {
      // this will trigger the listener but it will ignore it
      bloc.swapRoutes(RouteChange(configuration.routeName!,
          params: configuration.params ?? {}));
    }
  }

  @override
  GlobalKey<NavigatorState>? get navigatorKey => _navigatorKey;
}

class FHRouteInformationParser extends RouteInformationParser<FHRoutePath> {
  @override
  Future<FHRoutePath> parseRouteInformation(
      RouteInformation routeInformation) async {
    if (routeInformation.location == null) {
      return FHRoutePath()..routeName = '/loading';
    }

    final uri = Uri.parse(routeInformation.location!);

    if (uri.pathSegments.isEmpty) {
      return FHRoutePath()..routeName = '/loading';
    }

    if (uri.pathSegments.length == 1) {
      final path = uri.pathSegments[0];
      return FHRoutePath()..routeName = path.startsWith('/') ? path : '/$path';
    }

    return FHRoutePath()..routeName = '/loading';
  }

  @override
  RouteInformation restoreRouteInformation(FHRoutePath path) {
    return path.make();
  }
}
