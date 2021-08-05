import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';

class AndysScaffoldRoute extends StatefulWidget {
  const AndysScaffoldRoute({Key? key}) : super(key: key);

  @override
  _AndysScaffoldRouteState createState() => _AndysScaffoldRouteState();
}

class _AndysScaffoldRouteState extends State<AndysScaffoldRoute> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder(
      stream: BlocProvider.of<ManagementRepositoryClientBloc>(context)
          .redrawChangedStream,
      builder: (BuildContext context, AsyncSnapshot<RouteChange?> data) {
        if (data.hasData && data.data != null) {
          return ManagementRepositoryClientBloc.router
              .getRoute(data.data!.route)(context, data.data!.params);
        }

        return const SizedBox.shrink();
      },
    );
  }
}
