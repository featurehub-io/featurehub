

import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/superuser_common.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class AdminServiceAccountStepdefs {
  final SuperuserCommon common;
  final Shared shared;
  final UserCommon userCommon;

  AdminServiceAccountStepdefs(this.common, this.shared, this.userCommon);

  @Given(r'I create a admin service account called {string}')
  Future<void> iCreateANewAdminServiceAccount(String name) async {
    final details = await common.personService.createPerson(CreatePersonDetails(name: name, personType: PersonType.serviceAccount));

    // token can't be null
    shared.adminServiceAccountToken = details.token;
    shared.person = await common.personService.getPerson(details.personId);
  }

  @When(r'I have set the admin service account as the user')
  void iHaveSetTheAdminServiceAccountAsTheUser() async {
    userCommon.tokenized = TokenizedPerson(accessToken: shared.adminServiceAccountToken, person: shared.person);
  }

  @Then(r'I reset the admin service account token')
  void iResetTheAdminServiceAccountToken() async {
    final token = await common.personService.resetSecurityToken(shared.person.id!.id);
    shared.adminServiceAccountToken = token.token;
  }

  @Then(r'I cannot reset the admin service account token')
  void iCannotResetTheAdminServiceAccountToken() async {
    var failed = true;
    try {
      final token = await common.personService.resetSecurityToken(
          shared.person.id!.id);
    } catch (e) {
      failed = false;
    }
    assert(!failed, 'We should not have been able to reset the token');
  }


  @Then(r'I can find the admin service account {string} by searching')
  void iCanFindThePersonBySearching(String name) async {
    final result = await common.personService.findPeople(filter: name, personTypes: [PersonType.serviceAccount]);

    assert(result.people.any((element) => element.id!.id == shared.person.id!.id));
  }
}
