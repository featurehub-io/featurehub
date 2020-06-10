import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/theme/theme_data.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'api/client_api.dart';
import 'config/routes.dart';
import 'routes/landing_route.dart';

void main() async {
  runApp(BlocProvider(
      creator: (_context, _bag) {
        final bloc = ManagementRepositoryClientBloc();
        ManagementRepositoryClientBloc.router = Router();
        ManagementRepositoryClientBloc.router.mrBloc = bloc;
        Routes.configureRoutes(ManagementRepositoryClientBloc.router);
        return bloc;
      },
      child: FeatureHubApp()));
}

class FeatureHubApp extends StatelessWidget {
  // This widget is the root of the application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FeatureHub',
      theme: myTheme,
      home: LandingRoute(title: 'FeatureHub'),
    );
  }
}
