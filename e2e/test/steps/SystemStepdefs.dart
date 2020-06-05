import 'package:app_singleapp/superuser_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class SystemStepdefs {
  final SuperuserCommon common;

  SystemStepdefs(this.common);

  @Given(r'the system has been initialized')
  void theSystemHasBeenInitialized() async {
    await common.initialize();
  }

  @And(r'I am logged in as the initialized user')
  void iAmLoggedInAsTheInitializedUser() async {
    common.makeSuperuserCurrentUser();

    Person person = await common.personService.getPerson('self');
    assert(person.email == common.initUser);
  }


}
