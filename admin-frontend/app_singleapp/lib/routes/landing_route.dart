import 'dart:async';
import 'dart:html';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/routes/andys_scaffold_route.dart';
import 'package:app_singleapp/widgets/common/fh_scaffold.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:app_singleapp/widgets/setup/setup_widget.dart';
import 'package:app_singleapp/widgets/simple_widget.dart';
import 'package:app_singleapp/widgets/user/signin/signin_widget.dart';
import 'package:app_singleapp/widgets/user/update/password_reset_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

class LandingRoute extends StatefulWidget {
  final String title;

  const LandingRoute({Key key, this.title}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return LandingRouteState();
  }
}

class LandingRouteState extends State<LandingRoute> {
  Timer redirectTimer;

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
            client.customError(
                messageTitle:
                    'Error getting initial state'); //return FailureWidget(error: snapshot.error);
            widget = Text('Error MF!');
          } else if (snapshot.data == InitializedCheckState.initialized) {
            widget = Center(
                child: MediaQuery.of(context).size.width > 400
                    ? Container(
                        width: 500,
                        child: SigninWidget(),
                      )
                    : SigninWidget());
          } else if (snapshot.data ==
              InitializedCheckState.requires_password_reset) {
            widget = Center(
                child: MediaQuery.of(context).size.width > 400
                    ? Container(
                        width: 500,
                        child: ResetPasswordWidget(),
                      )
                    : ResetPasswordWidget());
          } else if (snapshot.data == InitializedCheckState.zombie) {
            ManagementRepositoryClientBloc.router
                .navigateTo(context, '/feature-status');
            widget = AndysScaffoldRoute();
          } else if (snapshot.data == InitializedCheckState.uninitialized) {
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
            widget = SimpleWidget(
              message: 'waiting for connection....',
            );
          }

          if (widget == null) {
            print("no widget");
            return SizedBox.shrink();
          }

          print("has widget");
          return FHScaffoldWidget(
            body: widget,
            bodyMainAxisAlignment: MainAxisAlignment.center,
          );
        });
  }

  @override
  void dispose() {
    super.dispose();
    if (redirectTimer != null) {
      redirectTimer.cancel();
    }
  }
}
