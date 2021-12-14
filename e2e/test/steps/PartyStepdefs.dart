import 'package:e2e_tests/events_common.dart';
import 'package:e2e_tests/shared.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:ogurets/ogurets.dart';

class PartyStepdefs {
  final EventsCommon eventsCommon;
  final Shared shared;

  PartyStepdefs(this.eventsCommon, this.shared);
  // PartyStepdefs(this.shared);

  String _apiKey() {
    return [
      'default',
      shared.environment.id,
      shared.serviceAccount.apiKeyServerSide
    ].join('/');
  }

  @Then(
      r'We listen for party server edge features from the shared service account')
  void weListenForPartyServerEdgeFeaturesFromTheSharedServiceAccount() async {
    eventsCommon.setAppSdkUrl(_apiKey());

    int count = 10;
    while (count > 0) {
      await Future.delayed(Duration(milliseconds: 400));
      if (eventsCommon.repository != null &&
          eventsCommon.repository!.readyness == Readyness.Ready) {
        break;
      }
      count--;
      print("counting down... ${count}");
    }
    assert(
        eventsCommon.repository != null &&
            eventsCommon.repository!.readyness == Readyness.Ready,
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
    assert(eventsCommon.repository != null,
        'You must have connected to the backend Edge');
    table.forEach((line) {
      final key = line['feature'].toString();
      FeatureStateHolder fsh = eventsCommon.repository!.getFeatureState(key);
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

  @Then(r'we do an OPTIONS check on the API to ensure we have CORS access')
  void weDoAnOPTIONSCheckOnTheAPIToEnsureWeHaveCORSAccess(
      GherkinTable table) async {
    assert(shared.serviceAccount.apiKeyServerSide != null,
        'The service account must have a server side key');
    final status = await eventsCommon.optionsCheck(_apiKey());
    print("OPTIONS headers are ${status.headers}");
    assert(status.statusCode == 200);
    final headerMap = status.headers.map;
    table.forEach((line) {
      final headerName = line['header']!.toLowerCase();
      final val = headerMap[headerName];
      assert(val != null && val.isNotEmpty, "Header ${headerName} is empty");
      String check = line['valueContains']!;
      if (val!.length == 1) {
        assert(val[0].contains(check),
            '${headerName}: ${val[0]} should equal ${check}');
      } else {
        check
            .split(',')
            .map((e) => e.trim())
            .where((e) => e.isNotEmpty)
            .forEach((part) {
          assert(val.contains(part), '$val does not contain $part');
        });
      }
    });
  }

  @And(r'we poll the current service account to full the repository')
  void wePollTheCurrentServiceAccountToFullTheRepository() async {
    assert(shared.serviceAccount.apiKeyServerSide != null,
        'The service account must have a server side key');
    await eventsCommon.pollRepository(_apiKey());
  }
}
