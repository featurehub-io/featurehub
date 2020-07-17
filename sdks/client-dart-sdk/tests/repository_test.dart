import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:test/test.dart';

void main() {
  ClientFeatureRepository repo;

  setUp(() {
    repo = ClientFeatureRepository();
  });

  test('Readyness should fire when features appear', () {
    final sub = repo.readynessStream;
  });
}
