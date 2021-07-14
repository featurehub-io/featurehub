import 'dart:async';

import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/widgets/common/fh_scaffold.dart';

const String routeSetupApp = 'setup';
const String routeLoginApp = 'login';

class FHRoutePath {
  final String _routeName;
  Map<String, List<String>> params;

  FHRoutePath(String routeName, {this.params = const {}})
      : _routeName = routeName.startsWith('/') ? routeName : '/$routeName';

  String get routeName => _routeName;

  RouteInformation make() {
    final p =
        params.entries.map((e) => '${e.key}=${e.value.join(',')}').join('&');
    return RouteInformation(location: _routeName + (p.isNotEmpty ? '?$p' : ''));
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
  FHRoutePath _path;
  RouteSlot _currentSlot = RouteSlot.loading;

  @override
  FHRoutePath get currentConfiguration {
    print('current config $_path slot is $_currentSlot');
    return _path;
  }

  @override
  Widget build(BuildContext context) {
    return Navigator(
      key: navigatorKey,
      pages: [routeWrapperPage(context)],
      onPopPage: (route, result) {
        if (!route.didPop(result)) {
          return false;
        }

        notifyListeners();

        return true;
      },
    );
  }

  MaterialPage routeWrapperPage(BuildContext context) {
    final handler = ManagementRepositoryClientBloc.router
        .forNamedRoute(_path.routeName, _currentSlot);

    final child = handler.handler.handlerFunc(context, _path.params);
    final wrapWidget = scaffoldWrapPermissions.contains(handler.permissionType);

    return MaterialPage(
        key: ValueKey('app'),
        child: wrapWidget ? RouteWrapperWidget(child: child) : child);
  }

  @override
  FHRouteDelegate(this.bloc)
      : _navigatorKey = GlobalKey<NavigatorState>(),
        _path =
            FHRoutePath(routeSlotMappings[RouteSlot.loading]!.initialRoute) {
    // notifyListeners simply causes the build() method above to be called.

    // TODO: note these listeners have NO cleanup. If a user changes them we will start getting two events

    // listen for an internal route change. This is someone calling ManagementRepositoryClientBloc.route.swapRoutes
    // which currently exists all over the code base.
    bloc.routeChangedStream.listen((r) {
      if (r != null) {
        print('route change request $r');
        if (_path.routeName != r.route ||
            (!const MapEquality().equals(_path.params, r.params) &&
                _path.routeName == r.route)) {
          _path = FHRoutePath(r.route, params: r.params);
          print('route change event to $_path');
          notifyListeners();
        }
      }
    });

    bloc.siteInitialisedStream.listen((s) {
      print('site init stream is $s vs $_currentSlot');
      if (s != _currentSlot) {
        _currentSlot = s;

        // if they have logged in now and have a held route, lets check if they are allowed to access it, and if so
        // swap to that instead
        if ((_currentSlot == RouteSlot.personal ||
                _currentSlot == RouteSlot.portfolio) &&
            _stashedRoutePath != null) {
          if (ManagementRepositoryClientBloc.router
              .canUseRoute(_stashedRoutePath!.routeName)) {
            print('can not use route, unstashing $_stashedRoutePath!');
            bloc.swapRoutes(RouteChange(_stashedRoutePath!.routeName,
                params: _stashedRoutePath!.params));
            _stashedRoutePath = null;
            return;
          } else {
            _stashedRoutePath = null;
            _path = FHRoutePath(routeSlotMappings[_currentSlot]!.initialRoute);
          }
        } else {
          _path = FHRoutePath(routeSlotMappings[_currentSlot]!.initialRoute);
        }

        notifyListeners();
      }
    });
  }

  FHRoutePath? _stashedRoutePath;

  @override
  Future<void> setNewRoutePath(FHRoutePath newPath) async {
    if (ManagementRepositoryClientBloc.router.canUseRoute(newPath.routeName)) {
      print('set new route path $newPath');
      _path = newPath;

      // this will trigger the listener but it will ignore it
      bloc.swapRoutes(RouteChange(newPath.routeName, params: newPath.params));
    } else {
      print('cant use route $newPath so stashing');
      _stashedRoutePath = newPath;
    }
  }

  @override
  GlobalKey<NavigatorState>? get navigatorKey => _navigatorKey;
}

class FHRouteInformationParser extends RouteInformationParser<FHRoutePath> {
  @override
  Future<FHRoutePath> parseRouteInformation(
      RouteInformation routeInformation) async {
    print('parsing route $routeInformation');
    if (routeInformation.location == null) {
      return FHRoutePath('/loading');
    }

    final uri = Uri.parse(routeInformation.location!);

    if (uri.pathSegments.isEmpty) {
      return FHRoutePath('/loading');
    }

    if (uri.pathSegments.length == 1) {
      final path = uri.pathSegments[0];
      final p = FHRoutePath(path, params: uri.queryParametersAll);
      print('parsed segs $p');
      return p;
    }

    return FHRoutePath('/loading');
  }

  @override
  RouteInformation restoreRouteInformation(FHRoutePath path) {
    return path.make();
  }
}
