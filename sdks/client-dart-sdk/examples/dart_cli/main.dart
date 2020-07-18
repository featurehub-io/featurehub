import 'dart:io';

import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:logging/logging.dart';

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

  final sdkUrl = Platform.environment['SDK_URL'];
  final sdkHost = Platform.environment['SDK_HOST'];

  if (sdkUrl == null || sdkHost == null) {
    print('Please set the SDK_URL and SDK_HOST values.');
    exit(-1);
  }

  final repo = ClientFeatureRepository();

  repo.readynessStream.listen((ready) {
    print('readyness $ready');
  });

  repo.newFeatureStateAvailableStream.listen((event) {
    repo.availableFeatures.forEach((key) {
      final feature = repo.getFeatureState(key);
      print(
          'feature ${key} is of type ${feature.type} and has the value ${feature.value}');
    });
  });

  final es = EventSourceRepositoryListener(sdkHost + '/' + sdkUrl, repo);

  print('hit <enter> to cancel');
  await stdin.first;

  es.close();
}
