import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

enum CreateUserForm { defaultState, successState }

class CreateUserBloc implements Bloc {
  RegistrationUrl registrationUrl;
  String email;
  String name;
  GlobalKey<FormState> formKey;

  final ManagementRepositoryClientBloc client;

  // main widget should respond to changes in this.
  final _formStateStream = rxdart.BehaviorSubject<CreateUserForm>();

  Stream<CreateUserForm> get formState => _formStateStream.stream;

  SelectPortfolioGroupBloc selectGroupBloc;

  @override
  void dispose() {
    _formStateStream.close();
  }

  CreateUserBloc(this.client, {this.selectGroupBloc}) : assert(client != null) {
    _formStateStream.add(CreateUserForm.defaultState);
  }

  Future<void> createUser(String email) {
    final listOfAddedPortfolioGroups =
        selectGroupBloc.listOfAddedPortfolioGroups;
    final cpd = CreatePersonDetails(email: email, name: name, groupIds: listOfAddedPortfolioGroups.where((pg) => pg.group != null).map((pg) => pg.group.id).toList(), );

    return client.personServiceApi.createPerson(cpd).then((data) {
      registrationUrl = data;
      registrationUrl.registrationUrl =
          client.rewriteUrl(registrationUrl.registrationUrl);

      _formStateStream.add(CreateUserForm.successState);

      selectGroupBloc.clearAddedPortfoliosAndGroups();
    });
  }

  void backToDefault() {
    _formStateStream.add(CreateUserForm.defaultState);
  }
}
