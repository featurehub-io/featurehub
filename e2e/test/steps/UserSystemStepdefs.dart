import 'package:e2e_tests/user_common.dart';
import 'package:ogurets/ogurets.dart';

/// all system level functions for a person should go in here, things that require no
/// authentication or require you to specify a user

class UserSystemStepdefs {
  final UserCommon _userCommon;

  UserSystemStepdefs(this._userCommon);

  @Then(r'I should be able to logout')
  void iShouldBeAbleToLogout() async {
    await _userCommon.authService.logout();
  }

  @And(r"the current user's password requires resetting")
  void theCurrentUserSPasswordRequiresResetting() async {
    assert(_userCommon.person.passwordRequiresReset == true,
        'Current person should need to reset their password');
  }
}
