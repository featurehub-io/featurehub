import 'package:collection/collection.dart' show IterableNullableExtension;
import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class UserStateStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  UserStateStepdefs(this.userCommon, this.shared);

  List<String>? storedEnvironments;

  List<String> filterEnvs(String envIds) {
    return envIds
        .split(",")
        .map((e) => e.trim())
        .where((element) => element.isNotEmpty)
        .toList();
  }

  @Then(r'I get the stored hidden environment ids and they are the same')
  void iGetTheEnvIdsAndTheyAreTheSame() async {
    final stored = await userCommon.userStateService
        .getHiddenEnvironments(shared.application.id!);

    assert(storedEnvironments != null, 'must store environment ids first');

    assert(stored.environmentIds.length == storedEnvironments!.length,
        'the environments dont match');

    stored.environmentIds.forEach((envId) {
      assert(storedEnvironments!.contains(envId),
          'envId in stored that did not exist in list we sent ${envId} vs ${storedEnvironments}');
    });
  }

  @Then(
      r'I cannot try and store hidden environment for the current application')
  void iCannotTryAndStoreHiddenEnvironmentForTheCurrentApplication() async {
    assert(storedEnvironments != null, 'must store environment ids first');

    try {
      await userCommon.userStateService.saveHiddenEnvironments(
          shared.application.id!,
          HiddenEnvironments(
            environmentIds: storedEnvironments,
          ));
      assert(false, 'Should not be able to save');
    } catch (e) {
      assert(e is ApiException, '$e is not a valid ApiException');
      assert((e as ApiException).code == 403);
    }
  }

  @And(
      r'I select the environments to hide {string} for the current application')
  void iSelectTheEnvironmentsToHideForTheCurrentApplication(
      String envNames) async {
    final envs = filterEnvs(envNames);

    final environments = (await Future.wait(envs.map((envName) async =>
            await userCommon.findExactEnvironment(
                envName, shared.application.id!))))
        .whereNotNull()
        .toList();

    storedEnvironments = environments.map((e) => e.id!).toList();
  }

  @When(r'I try and store hidden environments')
  void iTryAndStoreHiddenEnvironments() async {
    assert(storedEnvironments != null, 'must store environment ids first');

    await userCommon.userStateService.saveHiddenEnvironments(
        shared.application.id!,
        HiddenEnvironments(
          environmentIds: storedEnvironments,
        ));
  }
}
