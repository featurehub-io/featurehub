import 'package:app_singleapp/superuser_common.dart';
import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';


class PersonStepdefs {
  final UserCommon userCommon;
  final Shared shared;
  final SuperuserCommon common;

  PersonStepdefs(this.userCommon, this.shared, this.common);

  RegistrationUrl registrationUrl;

  /// this should be run by a user
  @And(r'complete their registration with name {string} and password {string} and email {string}')
  void completeTheirRegistration(String name, String password, String email) async {
    await userCommon.completeRegistration(name, password, email, registrationUrl.registrationUrl);
  }

  /// run in user space
  @Then(r'the user exists and has superuser groups')
  void theUserExistsAndHasSuperuserGroups() async {
    var person = await userCommon.personService.getPerson(userCommon.person.id.id, includeGroups: true);

    assert(person != null, 'person is null');
    assert(person.groups.firstWhere((g) => g.admin && g.portfolioId == null, orElse: () => null) != null, 'person is not superuser');
  }

  // run in user space as no-one should be logged in
  @And(r'I can login as user {string} with password {string}')
  void iCanLoginAsUser(String email, String password) async {
    userCommon.clearAuth(); // clear the current user (if any)

    var person = await userCommon.authService.login(UserCredentials()
      ..email = email
      ..password = password);

    assert(person != null, 'person cannot login with that password');

    userCommon.tokenized = person;
  }

  @When(r'My head falls off')
  void clearUserAuth() async { // clears the auth in the user space. Just to see if anyone actually reads this code.
    userCommon.clearAuth();
  }

  /// operates in user space
  @And(r'I can see the user has access to the portfolio')
  void iCanSeeTheUserHasAccessToThePortfolioGroup() async {
    assert(userCommon.person.groups.where((g) => g.id == shared.portfolioAdminGroup.id).toList().length == 1, 'Cannot find person in portfolio admin group');
  }

  /// operates in user space
  @When(r'I want to change their password from {string} to {string} I can')
  void iWantToChangeTheirPasswordFromToICan(String oldPassword, String newPassword) async {
    assert(userCommon.person != null, 'You need to have a valid person ready first');

    if (!userCommon.hasToken) {
      assert(userCommon.person != null, "we don't know which user to change the password of");
      await iCanLoginAsUser(userCommon.person.email, oldPassword);
    }

    var person = await userCommon.authService.changePassword(
      userCommon.person.id.id,
      PasswordUpdate()
        ..newPassword = newPassword
        ..oldPassword = oldPassword);

    assert(person.id.id == userCommon.person.id.id, 'Not the same person or the person change failed');
  }

  @And(r'I update the persons data from the host')
  void iUpdateThePersonsDataFromTheHost() async {
    var person = await userCommon.personService.getPerson('self', includeGroups: true, includeAcls: true);
    assert(person != null, 'could not find self');
    assert(person.id.id == userCommon.person.id.id, 'not the same person!!!');
    userCommon.person = person;
  }

  @When(r'I try an update the user {string} to a new email {string} and new name {string} I fail')
  void iTryAnUpdateTheUserToANewEmailAndNewNameIFail(String email, String newemail, String newname) async {
    var user = await userCommon.findExactEmail(email, personServiceApi: common.personService);
    assert(user != null, 'Cannot find user to try and update');

    var person = null;
    try {
      person = await userCommon.personService.updatePerson(user.id.id,
        Person()
          ..name = newname
          ..email = newemail);
    } catch (e) {
      print("step failed as it should");
    }

    assert(person == null, 'A normal user should not be able to update a user');
  }

  @And(r'then I reset my temporary password to {string}')
  void thenIResetMyTemporaryPasswordTo(String password) async {
    var tp = await userCommon.authService.replaceTempPassword(userCommon.person.id.id, PasswordReset()..password = password);
    userCommon.tokenized = tp;
  }

  @And(r'then I cannot reset my temporary password to {string}')
  void thenICannotResetMyTemporaryPasswordTo(String password) async {
    var tp;

    assert((userCommon.person.passwordRequiresReset ?? false) == false, 'User is in password reset mode!');

    try {
      tp = await userCommon.authService.replaceTempPassword(
        userCommon.person.id.id, PasswordReset()
        ..password = password);
    } catch (e) {}

    assert(tp == null, 'was able to change password without being in password reset mode');
  }

  @And(r'I load the person {string}')
  void iLoadThePerson(String email) async {
    final person = await userCommon.personService.getPerson('self', includeAcls: true, includeGroups: true);
    shared.person = person;
  }
}
