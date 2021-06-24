import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/config/route_handlers.dart';
import 'package:app_singleapp/routes/andys_scaffold_route.dart';
import 'package:app_singleapp/widget_creator.dart';
import 'package:app_singleapp/widgets/common/fh_scaffold.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:app_singleapp/widgets/setup/setup_widget.dart';
import 'package:app_singleapp/widgets/simple_widget.dart';
import 'package:app_singleapp/widgets/user/update/password_reset_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import '../api/client_api.dart';

class LandingRoute extends StatefulWidget {
  final String title;

  LandingRoute({Key? key, required this.title}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return LandingRouteState();
  }
}

class LandingRouteState extends State<LandingRoute> {
  Timer? redirectTimer;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return _homeStreamBuilder(bloc);
  }

  Widget _homeStreamBuilder(ManagementRepositoryClientBloc client) {
    return StreamBuilder<InitializedCheckState>(
        stream: client.initializedState,
        builder: (BuildContext context,
            AsyncSnapshot<InitializedCheckState> snapshot) {
          Widget widget;

          if (snapshot.hasError) {
            client.customError(messageTitle: 'Error getting initial state');
            widget = Text('Error!');
          } else if (snapshot.data == preRouterStateInitialized) {
            if (client.currentRoute != null &&
                client.currentRoute!.route == '/register-url') {
              widget = routeCreator.registerUrl(client,
                  params: client.currentRoute!.params);
            } else {
              widget = Center(
                  child: MediaQuery.of(context).size.width > 400
                      ? Container(
                          width: 500,
                          child: widgetCreator.createSigninWidget(client),
                        )
                      : widgetCreator.createSigninWidget(client));
            }
          } else if (snapshot.data == preRouterStateRequiresPasswordReset) {
            widget = Center(
                child: MediaQuery.of(context).size.width > 400
                    ? Container(
                        width: 500,
                        child: ResetPasswordWidget(),
                      )
                    : ResetPasswordWidget());
          } else if (snapshot.data == preRouterStateZombie) {
            var currentRoute = client.currentRoute;
            ManagementRepositoryClientBloc.router.navigateTo(context,
                currentRoute != null ? currentRoute.route : '/applications',
                params: currentRoute?.params ?? {});
            widget = AndysScaffoldRoute();
          } else if (snapshot.data == preRouterStateUninitialized) {
            widget = Center(
                child: MediaQuery.of(context).size.width > 500
                    ? Container(
                        width: 500,
                        child: BlocProvider<SetupBloc>(
                            creator: (_context, _bag) => SetupBloc(client),
                            child: SetupPageWidget()),
                      )
                    : BlocProvider<SetupBloc>(
                        creator: (_context, _bag) => SetupBloc(client),
                        child: SetupPageWidget()));
          } else {
            widget = unknownStateToWidget(snapshot.data, client);
          }

          return FHScaffoldWidget(
            body: widget,
            bodyMainAxisAlignment: MainAxisAlignment.center,
          );
        });
  }

  Widget unknownStateToWidget(
      String? state, ManagementRepositoryClientBloc client) {
    return SimpleWidget(
      message: 'waiting for connection....',
    );
  }

  @override
  void dispose() {
    super.dispose();
    redirectTimer?.cancel();
  }
}
