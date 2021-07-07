import 'package:open_admin_app/theme/theme_data.dart';
import 'package:open_admin_app/widgets/dynamic-theme/fh_dynamic_theme.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';

import 'api/client_api.dart';
import 'routes/landing_route.dart';

final _log = Logger('mr_app');

void main() async {
  Logger.root.level = Level.ALL; // defaults to Level.INFO
  Logger.root.onRecord.listen((record) {
    // ignore: avoid_print
    print('${record.level.name}: ${record.time}: ${record.message}');
    if (record.object != null) {
      // ignore: avoid_print
      print('exception:${record.object}');
    }
    if (record.stackTrace != null) {
      // ignore: avoid_print
      print('stackTrace:${record.stackTrace}');
    }
  });
  try {
    await mainApp();
  } catch (e, s) {
    _log.severe('Failed', e, s);
  }
}

Future<void> mainApp() async {
  runApp(BlocProvider(
      creator: (_context, _bag) {
        return ManagementRepositoryClientBloc();
      },
      child: FeatureHubApp()));
}

class FeatureHubApp extends StatelessWidget {
  // This widget is the root of the application.
  @override
  Widget build(BuildContext context) {
    return DynamicTheme(
        defaultBrightness: Brightness.light,
        data: (brightness) =>
            brightness == Brightness.light ? myTheme : darkTheme,
        themedWidgetBuilder: (context, theme) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            title: 'FeatureHub',
            theme: theme,
            home: LandingRoute(title: 'FeatureHub'),
            onGenerateRoute: (RouteSettings settings) {
              if (settings.name == null) {
                return null;
              }

              final uri = Uri.parse(settings.name!);
              final params = uri.queryParametersAll;
              ManagementRepositoryClientBloc.router
                  .navigateTo(context, uri.path, params: params);
              BlocProvider.of<ManagementRepositoryClientBloc>(context)
                  .resetInitialized();
              return null;
            },
          );
        });
  }
}
