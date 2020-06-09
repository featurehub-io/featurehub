import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

// because Andy asked where the Scaffold was. It should be easy to find.
// ironically the scaffold is in the LandingRoute
class AndysScaffoldRoute extends StatefulWidget {
  @override
  _AndysScaffoldRouteState createState() => _AndysScaffoldRouteState();
}

class _AndysScaffoldRouteState extends State<AndysScaffoldRoute> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder(
      stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
          .routeChangedStream,
      builder: (BuildContext context, AsyncSnapshot<RouteChange> data) {
        if (data.hasData) {
          return ManagementRepositoryClientBloc.router
              .getRoute(data.data.route)(context, data.data.params);
        }

        return SizedBox.shrink();
      },
    );
  }
}
