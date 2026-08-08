import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/password_policy_validator.dart';
import 'package:rxdart/rxdart.dart';

enum RegisterUrlForm {
  loadingState,
  initialState,
  errorState,
  successState,
  alreadyLoggedIn
}

class RegisterBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  String? token;
  Person? person;
  String? name;
  String? password;

  // main widget should respond to changes in this.
  final _formStateStream = BehaviorSubject<RegisterUrlForm>();
  Stream<RegisterUrlForm> get formState => _formStateStream.stream;

  RegisterBloc(this.mrClient) {
    _formStateStream.add(RegisterUrlForm.loadingState);
  }

  // get the email address from the token
  void getDetails(String token) {
    if (token != this.token) {
      mrClient.authServiceApi.personByToken(token).then((data) {
        if (data.additional
                ?.firstWhereOrNull((pi) => pi.key == 'already-logged-in') !=
            null) {
          // we can get into a situation where a person is already "good"
          // but their registration link still works, so lets redirect to login
          _formStateStream.add(RegisterUrlForm.alreadyLoggedIn);
        } else {
          person = data;
          this.token = token;
          _formStateStream.add(RegisterUrlForm.initialState);
        }
      }).catchError((e) {
        _formStateStream.addError(e);
      });
    }
  }

  /// Completes registration.
  ///
  /// Returns the rules the server rejected the password for, or null if it succeeded or failed for
  /// some other reason. The server is the authority on the password policy, so a rejection here is
  /// shown against the field rather than as a generic error dialog.
  Future<List<PasswordPolicyRule>?> completeRegistration(String token,
      String email, String name, String password, String confirmPassword) async {
    try {
      final data = await mrClient.authServiceApi.registerPerson(
          PersonRegistrationDetails(
        email: email,
        password: password,
        confirmPassword: confirmPassword,
        name: name,
        registrationToken: token,
      ));

      if (!_formStateStream.isClosed) {
        _formStateStream.add(RegisterUrlForm.successState);
      }

      await mrClient.hasToken(data);
      return null;
    } catch (e, s) {
      final rejected = passwordPolicyRejection(e);
      if (rejected != null) {
        return rejected;
      }

      mrClient.dialogError(e, s);
      return null;
    }
  }

  @override
  void dispose() {
    _formStateStream.close();
  }
}
