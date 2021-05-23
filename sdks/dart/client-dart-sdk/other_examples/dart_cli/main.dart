import 'dart:io';

import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub_io.dart';
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
    // ignore: avoid_print
    print('Please set the SDK_URL and SDK_HOST values.');
    exit(-1);
  }

  final repo = ClientFeatureRepository();

  repo.readynessStream.listen((ready) {
    // ignore: avoid_print
    print('readyness $ready');
  });

  repo.clientContext
      .userKey(Platform.environment['USERKEY'] ?? 'jemima')
      .device(StrategyAttributeDeviceName.desktop)
      .platform(StrategyAttributePlatformName.macos)
      .attr('sausage', 'kielbasa')
      .build();

  repo.newFeatureStateAvailableStream.listen((event) {
    repo.availableFeatures.forEach((key) {
      final feature = repo.feature(key);
      // ignore: avoid_print
      print(
          'feature ${key} is of type ${feature.type} and has the value ${feature.value}');
    });
  });

  final es = EventSourceRepositoryListener(sdkHost, sdkUrl, repo);

  // ignore: avoid_print
  print('hit <enter> to cancel');
  await stdin.first;

  es.close();
}
