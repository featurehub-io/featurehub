import 'dart:async';
import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;


enum RegisterUrlForm { loadingState, initialState, errorState, successState }

class RegisterBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  String token;
  Person person;
  String name;
  String password;

  // main widget should respond to changes in this.
  final _formStateStream = rxdart.BehaviorSubject<RegisterUrlForm>();

  Stream<RegisterUrlForm> get formState => _formStateStream.stream;

  RegisterBloc(this.mrClient) : assert(mrClient != null) {
    _formStateStream.add(RegisterUrlForm.loadingState);
    print("RegisterBloc init");
  }

  // get the email address from the token
  void getDetails(String token) {
    if (token != this.token) {
      mrClient.authServiceApi.personByToken(token).then((data) {
        person = data;
        this.token = token;
        _formStateStream.add(RegisterUrlForm.initialState);
      }).catchError((e) {
        _formStateStream.addError(e);
      });
    }
  }

  // complete the registration process
  void completeRegistration(String token,String email, String name, String password, String confirmPassword) {
    mrClient.authServiceApi.registerPerson(PersonRegistrationDetails()
      ..email = email
      ..password = password
      ..confirmPassword = confirmPassword
      ..name = name
      ..registrationToken = token).then((data) {
      _formStateStream.add(RegisterUrlForm.successState);
    }).catchError((e, s) {
      mrClient.dialogError(e, s);
    });
  }

  void dispose() {
    _formStateStream.close();
  }
}
