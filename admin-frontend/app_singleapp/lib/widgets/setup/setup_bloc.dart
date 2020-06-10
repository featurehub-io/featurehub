import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

/// this stores the state for the setup screen
class SetupContext extends InheritedWidget {
  SetupContext({Key key, @required Widget child})
      : assert(child != null),
        super(key: key, child: child);

  @override
  bool updateShouldNotify(InheritedWidget oldWidget) {
    return true;
  }

  static SetupContext of(BuildContext context) {
    return context.dependOnInheritedWidgetOfExactType(aspect: SetupContext);
  }
}

enum SetupPage { page1, page2, page3 }

class SetupBloc implements Bloc {
  String name;
  String email;
  String pw1;
  String pw2;
  String orgName;
  String portfolio;

  final ManagementRepositoryClientBloc mrClient;

  // main widget should respond to changes in this.
  final _pageSource = rxdart.BehaviorSubject<SetupPage>();
  Stream<SetupPage> get pageState => _pageSource.stream;

  final _setupSource = rxdart.BehaviorSubject<bool>();
  Stream<bool> get setupState => _setupSource.stream;

  SetupPage current;

  SetupBloc(this.mrClient) : assert(mrClient != null) {
    current = SetupPage.page1;
    _pageSource.add(current);
  }

  void _setup() {
    _setupSource.add(false); // busy

    var s = SetupSiteAdmin();

    s.name = name;
    s.emailAddress = email;
    s.password = pw1;
    s.organizationName = orgName;
    s.portfolio = portfolio;

    mrClient.setupApi.setupSiteAdmin(s).then((data) {
      _setupSource.add(true);
      mrClient.setBearerToken(data.accessToken);
      mrClient.setPerson(data.person);
      //    client.setGroup(data.person.groups);
    }).catchError((e, s) => _setupSource.addError(e.toString()));
  }

  void reinitialize() {
    mrClient.isInitialized();
  }

  void nextPage() {
    if (current == SetupPage.page1) {
      current = SetupPage.page2;
    } else if (current == SetupPage.page2) {
      _setup(); // trigger setup and transition to next page

      current = SetupPage.page3;
    }

    _pageSource.add(current);
  }

  void priorPage() {
    // 3 & 1 can't go back
    if (current == SetupPage.page2) {
      current = SetupPage.page1;
    }

    _pageSource.add(current);
  }

  @override
  void dispose() {
    _pageSource.close();
    _setupSource.close();
  }
}
