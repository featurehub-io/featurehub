import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class UserStateStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  UserStateStepdefs(this.userCommon, this.shared);

  List<String> filterEnvs(String envIds) {
    return envIds
        .split(",")
        .map((e) => e.trim())
        .where((element) => element.isNotEmpty)
        .toList();
  }

  @When(
      r'I try and store hidden environment {string} for the current application')
  void iTryAndStoreHiddenEnvironmentForTheCurrentApplication(
      String envIds) async {
    List<String> envs = filterEnvs(envIds);

    await userCommon.userStateService.saveHiddenEnvironments(
        shared.application.id!,
        HiddenEnvironments(
          environmentIds: envs,
        ));
  }

  @Then(r'I get the env ids {string} and they are the same')
  void iGetTheEnvIdsAndTheyAreTheSame(String envIds) async {
    List<String> envs = filterEnvs(envIds);

    final stored = await userCommon.userStateService
        .getHiddenEnvironments(shared.application.id!);

    assert(envs
            .where((envId) => stored.environmentIds.contains(envId))
            .toList()
            .length ==
        envs.length);
  }

  @Then(
      r'I cannot try and store hidden environment for the current application')
  void iCannotTryAndStoreHiddenEnvironmentForTheCurrentApplication() async {
    try {
      await userCommon.userStateService.saveHiddenEnvironments(
          shared.application.id!,
          HiddenEnvironments(
            environmentIds: ['1', '2'],
          ));
      assert(false, 'Should not be able to save');
    } catch (e) {
      assert(e is ApiException);
      assert((e as ApiException).code == 403);
    }
  }
}
