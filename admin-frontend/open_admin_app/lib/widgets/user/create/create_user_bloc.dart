import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

enum CreateUserForm { defaultState, successState, loadingState }

class CreateUserBloc implements Bloc {
  RegistrationUrl? registrationUrl;
  String? email;
  String? name;
  GlobalKey<FormState>? formKey;

  final ManagementRepositoryClientBloc client;

  // main widget should respond to changes in this.
  final _formStateStream = rxdart.BehaviorSubject<CreateUserForm>();

  Stream<CreateUserForm> get formState => _formStateStream.stream;

  SelectPortfolioGroupBloc selectGroupBloc;

  @override
  void dispose() {
    _formStateStream.close();
  }

  CreateUserBloc(this.client, {required this.selectGroupBloc}) {
    _formStateStream.add(CreateUserForm.defaultState);
  }

  Future<void> createUser(String? email, String? name) {
    final listOfAddedPortfolioGroups =
        selectGroupBloc.listOfAddedPortfolioGroups;
    final cpd = CreatePersonDetails(
      email: email,
      name: name,
      personType: name == null ? PersonType.person : PersonType.serviceAccount,
      groupIds: listOfAddedPortfolioGroups
          .map((pg) => pg.group.id)
          .whereNotNull()
          .toList(),
    );

    return client.personServiceApi.createPerson(cpd).then((data) {
      registrationUrl = data;

      if (registrationUrl != null) {

        _formStateStream.add(CreateUserForm.successState);

        selectGroupBloc.clearAddedPortfoliosAndGroups();
      }
    });
  }

  void backToDefault() {
    _formStateStream.add(CreateUserForm.defaultState);
  }

  void loading() {
     _formStateStream.add(CreateUserForm.loadingState);
  }
}
