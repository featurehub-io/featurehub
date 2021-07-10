import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/config/route_handlers.dart';
import 'package:open_admin_app/routes/home_route.dart';
import 'package:open_admin_app/widgets/common/fh_scaffold.dart';
import 'package:open_admin_app/widgets/setup/setup_bloc.dart';
import 'package:open_admin_app/widgets/setup/setup_widget.dart';

import '../widget_creator.dart';

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

    return RouteInformation(location: "404");
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
  final GlobalKey<NavigatorState> navigatorKey;
  final ManagementRepositoryClientBloc bloc;
  bool isLoggedIn = false;
  bool? isInitialised;
  FHRoutePath? path;
  late StreamSubscription<Person> personStateListener;
  late StreamSubscription<bool?> siteInitializedListener;

  @override
  FHRoutePath get currentConfiguration {
    if (isInitialised == null) return FHRoutePath()..routeName = '/loading';
    if (isInitialised == false) return FHRoutePath()..routeName = '/setup';
    if (isInitialised == true && isLoggedIn == false)
      return FHRoutePath()..routeName = '/login';

    return path == null ? (FHRoutePath()..routeName = '/app') : path!;
  }

  @override
  Widget build(BuildContext context) {
    if (isInitialised == true && isLoggedIn == true) {
      print("path is ${path?.routeName}");
    }

    return Navigator(
      key: navigatorKey,
      pages: [
        if (isInitialised == null)
          MaterialPage(
              key: ValueKey('loading'), child: HomeRoute(title: 'Loading')),
        if (isInitialised == false)
          MaterialPage(key: ValueKey('setup'), child: SetupWrapperWidget()),
        if (isInitialised == true && isLoggedIn == false)
          MaterialPage(key: ValueKey('login'), child: SigninWrapperWidget()),
        if (isInitialised == true && isLoggedIn == true)
          MaterialPage(
              key: ValueKey('app'),
              child: RouteWrapperWidget(
                  child: path?.routeName == null
                      ? routeCreator.apps(bloc)
                      : ManagementRepositoryClientBloc
                          .router.handlers[path!.routeName!]!.handler
                          .handlerFunc(context, {}))),
        // MaterialPage(
        //   key: ValueKey('UnknownPage'),
        //   child: HomeRoute(title: 'No idea what you were looking for')),
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

  @override
  FHRouteDelegate(this.bloc) : navigatorKey = GlobalKey<NavigatorState>() {
    personStateListener = bloc.personState.personStream.listen((p) {
      if (bloc.isLoggedIn != this.isLoggedIn) {
        this.isLoggedIn = bloc.isLoggedIn;
        notifyListeners();
      }
    });

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

    siteInitializedListener = bloc.siteInitialisedSource.listen((s) {
      if (s != isInitialised) {
        isInitialised = s;
        notifyListeners();
      }
    });
  }

  @override
  Future<void> setNewRoutePath(FHRoutePath configuration) async {
    this.path = configuration;

    if (isInitialised == true && isLoggedIn) {
      // this will trigger the listener but it will ignore it
      bloc.swapRoutes(RouteChange(configuration.routeName!,
          params: configuration.params ?? {}));
    }
  }
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
      String path = uri.pathSegments[0];
      return FHRoutePath()..routeName = path.startsWith('/') ? path : '/$path';
    }

    return FHRoutePath()..routeName = '/loading';
  }

  @override
  RouteInformation restoreRouteInformation(FHRoutePath path) {
    return path.make();
  }
}

class SigninWrapperWidget extends StatelessWidget {
  const SigninWrapperWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final client = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return FHScaffoldWidget(
      bodyMainAxisAlignment: MainAxisAlignment.center,
      body: Center(
          child: MediaQuery.of(context).size.width > 400
              ? Container(
                  width: 500,
                  child: widgetCreator.createSigninWidget(client),
                )
              : widgetCreator.createSigninWidget(client)),
    );
  }
}

class SetupWrapperWidget extends StatelessWidget {
  const SetupWrapperWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final client = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return FHScaffoldWidget(
      bodyMainAxisAlignment: MainAxisAlignment.center,
      body: Center(
          child: MediaQuery.of(context).size.width > 500
              ? Container(
                  width: 500,
                  child: BlocProvider<SetupBloc>(
                      creator: (_context, _bag) => SetupBloc(client),
                      child: SetupPageWidget()),
                )
              : BlocProvider<SetupBloc>(
                  creator: (_context, _bag) => SetupBloc(client),
                  child: SetupPageWidget())),
    );
  }
}
