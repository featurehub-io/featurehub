import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/superuser_common.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

// person stepdefs requiring the admin user should be here
class AdminPersonStepdefs {
  final SuperuserCommon common;
  final UserCommon userCommon;
  final Shared shared;

  AdminPersonStepdefs(this.common, this.shared, this.userCommon);

  /// this must be run by a superuser
  @When(r'I register a new user with email {string}')
  void registerNewUser(String email) async {
    await common.initialize();
    shared.registrationUrl =
        await common.personService.createPerson(CreatePersonDetails()
          ..email = email
          ..name = email
          ..groupIds = []);
  }

  // must be run by supervisor
  @When(r'I register a new superuser with email {string}')
  void registerNewSuperuser(String email) async {
    await common.initialize();

    shared.registrationUrl =
        await common.personService.createPerson(CreatePersonDetails()
          ..email = email
          ..name = email
          ..groupIds = [common.superuserGroupId]);
  }

  @When(r'I register a new user with email {string}')
  void iRegisterANewUserWithEmail(String email) async {
    await common.initialize();

    shared.registrationUrl =
        await common.personService.createPerson(CreatePersonDetails()
          ..email = email
          ..name = email);
  }

  @When(r'the first superuser is used for authentication')
  void superuserIsUser() async {
    await common.initialize();

    userCommon.tokenized =
        common.tokenizedPerson; // make the userCommon the super user
  }

  @Given(
      r'^I have a randomly generated (person|superuser) with the start of name "(.*)"$')
  void randomlyGeneratedPerson(String type, String name) async {
    String uname =
        name + "_" + DateTime.now().millisecondsSinceEpoch.toString();

    final email = uname + "@mailinator.com";
    final password = name;

    await common.initialize();

    if (type == "person") {
      await registerNewUser(email);
    } else {
      await registerNewSuperuser(email);
    }

    await userCommon.completeRegistration(
        name, password, email, shared.registrationUrl.registrationUrl);

    shared.tokenizedPerson = await common.authService.login(UserCredentials()
      ..email = email
      ..password = password);

    shared.person =
        await common.personService.getPerson(email, includeGroups: true);
  }

  @And(r'The shared person is the authenticated person')
  void sharedPersonIsAuthenticated() {
    assert(shared.tokenizedPerson != null, 'no logged in shared person!');
    userCommon.tokenized = shared.tokenizedPerson;
  }

  // this checks to see if we have  the user already, so it has to belong to the superuser, but
  // the completion of registration is run as a normal user.
  @Given(
      r'^I have a fully registered (person|superuser) "(.*)" with email "(.*)" and password "(.*)"$')
  void iHaveAFullyRegisteredPersonWithEmailAndPassword(
      String type, String name, String email, String password) async {
    await common.initialize();

    SearchPersonResult spr =
        await common.personService.findPeople(filter: email);

    if (spr.people.isEmpty) {
      if (type == "person") {
        await registerNewUser(email);
      } else {
        await registerNewSuperuser(email);
      }

      await userCommon.completeRegistration(
          name, password, email, shared.registrationUrl.registrationUrl);
    } else {
      final person = spr.people[0];
      await common.authService.resetPassword(
          person.id.id,
          PasswordReset()
            ..password = 'password'
            ..reactivate = true);
      await common.authService.changePassword(
          person.id.id,
          PasswordUpdate()
            ..oldPassword = 'password'
            ..newPassword = password);

      shared.tokenizedPerson = await common.authService.login(UserCredentials()
        ..email = email
        ..password = password);
      userCommon.tokenized = shared.tokenizedPerson;
    }

    spr = await common.personService.findPeople(filter: email);
    userCommon.person = spr.people[0];
    shared.person = spr.people[0];
  }

  @Given(r'The administrator ensures the registered person {string} is deleted')
  void iDeleteTheRegisteredPerson(String email) async {
    await common.initialize();
    await common.personService.deletePerson(email, includeGroups: true);

    var user = await userCommon.findExactEmail(email,
        personServiceApi: common.personService);

    assert(user == null, 'Person still exists after deleting');
  }

  @And(
      r'the administrator updates user {string} to a new email {string} and new name {string}')
  void theAdministratorUpdatesUserToANewEmailAndNewName(
      String email, String newemail, String newname) async {
    await common.initialize();
    var user = await userCommon.findExactEmail(email,
        personServiceApi: common.personService);
    assert(user != null, 'Cannot find user to try and update');

    final emailGoingTo = await userCommon.findExactEmail(newemail,
        personServiceApi: common.personService);
    if (emailGoingTo != null) {
      await common.personService.updatePerson(
          emailGoingTo.id.id,
          emailGoingTo.copyWith(
              email: 'nonsense-' +
                  DateTime.now().millisecondsSinceEpoch.toString() +
                  '@mailinator.com'));
    }

    await common.personService.updatePerson(
        user.id.id, user.copyWith(name: newname, email: newemail));
//        Person()
//          ..version = user.version
//          ..name = newname
//          ..email = newemail);
  }

  @When(r'the administrator resets the password for {string} to {string}')
  void theAdministratorResetsThePasswordForTo(
      String email, String password) async {
    await common.initialize();
    var user = await userCommon.findExactEmail(email,
        personServiceApi: common.personService);
    assert(user != null, 'Cannot find user to try and update');
    await common.authService
        .resetPassword(user.id.id, PasswordReset()..password = password);
  }
}
