import 'package:logging/logging.dart';

import 'api/client_api.dart';
import 'main.dart' as core;

void main() async {
  overrideOrigin = false;

  Logger.root.level = Level.SEVERE; // defaults to Level.INFO
  Logger.root.onRecord.listen((record) {
    // ignore: avoid_print
    print('${record.level.name}: ${record.time}: ${record.message}');
  });

  core.mainApp();
}
