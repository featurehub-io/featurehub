import 'package:e2e_tests/events_common.dart';
import 'package:e2e_tests/shared.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:ogurets/ogurets.dart';

//final _log = Logger('PartyStepdefs');

class PartyStepdefs {
  final EventsCommon eventsCommon;
  final Shared shared;

  PartyStepdefs(this.eventsCommon, this.shared);

  @Then(
      r'We listen for party server edge features from the shared service account')
  void weListenForPartyServerEdgeFeaturesFromTheSharedServiceAccount() async {
    assert(shared.serviceAccount != null, 'no current service account');
    assert(shared.environment != null, 'no environment');
    eventsCommon.setAppSdkUrl([
      'default',
      shared.environment.id,
      shared.serviceAccount.apiKeyServerSide
    ].join('/'));

    int count = 10;
    while (count > 0) {
      await Future.delayed(Duration(milliseconds: 400));
      if (eventsCommon.repository.readyness == Readyness.Ready) {
        break;
      }
      count--;
      print("counting down... ${count}");
    }
    assert(eventsCommon.repository.readyness == Readyness.Ready,
        'Repository failed to become ready.');
  }

  @And(r'I wait for {int} seconds')
  void iWaitForSeconds(int sec) async {
    await Future.delayed(Duration(seconds: sec));
  }

  ///       | feature       | valueBoolean |
  //      | FEATURE_PARTY | false        |
  @And(r'the feature repository reports the following:')
  void theFeatureRepositoryReportsTheFollowing(GherkinTable table) async {
    table.forEach((line) {
      final key = line['feature'].toString();
      FeatureStateHolder fsh = eventsCommon.repository.getFeatureState(key);
      assert(fsh != null, 'Did not find feature $key');
      assert(fsh.key == key, 'Keys do not match! $key vs ${fsh.key}');
      if (line['valueBoolean'] != null &&
          line['valueBoolean'].toString().trim().isNotEmpty) {
        bool expected = line['valueBoolean'].toString().trim() == 'true';
        assert(fsh.booleanValue == expected,
            'Value was not as expected - ${fsh.booleanValue} and not ${expected}');
      }
    });
  }
}
