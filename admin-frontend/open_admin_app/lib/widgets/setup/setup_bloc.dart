// ignore: avoid_web_libraries_in_flutter
import 'dart:html';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

enum SetupPage { page1, page2, page3 }

class SetupBloc implements Bloc {
  String? name;
  String? email;
  String? pw1;
  String? pw2;
  String? orgName;
  String? portfolio;
  String? provider;

  final ManagementRepositoryClientBloc mrClient;

  // main widget should respond to changes in this.
  final _pageSource = rxdart.BehaviorSubject<SetupPage?>();
  Stream<SetupPage?> get pageState => _pageSource.stream;

  final _setupSource = rxdart.BehaviorSubject<bool>();
  Stream<bool> get setupState => _setupSource.stream;

  late SetupPage current;

  late bool _canGoToPage1;

  // tell UI whether or not the "prev" button is available on page 2
  bool get canGoToPage1 => _canGoToPage1;
  bool get hasLocal => mrClient.identityProviders.hasLocal;
  bool get has3rdParty => mrClient.identityProviders.has3rdParty;
  List<String> get externalProviders =>
      mrClient.identityProviders.externalProviders;
  IdentityProviderInfo? identityInfo(String provider) {
    return mrClient.identityProviders.identityInfo[provider];
  }

  SetupBloc(this.mrClient) {
    // go to page 1 if local or # of providers is > 2
    _canGoToPage1 =
        (hasLocal || mrClient.identityProviders.hasMultiple3rdPartyProviders);

    if (_canGoToPage1) {
      current = SetupPage.page1;
    } else {
      // we skip to here
      current = SetupPage.page2;
    }

    _pageSource.add(current);
  }

  void _setup() {
    _setupSource.add(false); // busy

    var s = SetupSiteAdmin(
        organizationName: orgName!,
        name: name,
        emailAddress: email,
        password: pw1,
        portfolio: portfolio!,
        authProvider: provider);

    mrClient.setupApi.setupSiteAdmin(s).then((data) {
      _setupSource.add(true);

      if (data.redirectUrl != null) {
        window.location.href = data.redirectUrl!;
      } else {
        mrClient.setBearerToken(data.accessToken);
        mrClient.setPerson(data.person!);
      }
      //    client.setGroup(data.person.groups);
    }).catchError((e, s) {
      _setupSource.addError(e.toString());
    });
  }

  void reinitialize() {
    mrClient.isInitialized();
    // automatically open the stepper to help them on their merry way
    mrClient.stepperOpened = true;
  }

  void nextPage() {
    if (current == SetupPage.page1) {
      current = SetupPage.page2;
    } else if (current == SetupPage.page2) {
      if (portfolio != null &&
          orgName != null &&
          portfolio!.trim().isNotEmpty &&
          orgName!.trim().isNotEmpty) {
        _setup(); // trigger setup and transition to next page

        current = SetupPage.page3;
      }
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
