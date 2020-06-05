import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:app_singleapp/widgets/user/common/portfolio_group.dart';
import 'package:app_singleapp/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

enum CreateUserForm { defaultState, successState }

class CreateUserBloc implements Bloc {
  RegistrationUrl registrationUrl;
  String email;
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
    Set<PortfolioGroup> listOfAddedPortfolioGroups = selectGroupBloc.listOfAddedPortfolioGroups;
    print('In create user');
    CreatePersonDetails cpd = CreatePersonDetails();
    cpd.email = email;
    cpd.groupIds = listOfAddedPortfolioGroups.map((pg) => pg.group.id).toList();
    print('Added group ids ...${cpd.groupIds.toString()}');
    return client.personServiceApi.createPerson(cpd).then((data) {
      _formStateStream.add(CreateUserForm.successState);
      registrationUrl = data;
      selectGroupBloc.clearAddedPortfoliosAndGroups();
    });
  }

  void backToDefault() {
    _formStateStream.add(CreateUserForm.defaultState);
  }



}
