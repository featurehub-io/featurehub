import 'package:app_singleapp/events_common.dart';
import 'package:app_singleapp/shared.dart';
import 'package:ogurets/ogurets.dart';

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
      shared.serviceAccount.apiKey
    ].join('/'));
    print("waiting");
    await Future.delayed(Duration(seconds: 6));
  }

  @And(r'I wait for {int} seconds')
  void iWaitForSeconds(int sec) async {
    await Future.delayed(Duration(seconds: sec));
  }
}
