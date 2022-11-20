import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';

class SearchPersonEntry {
  final SearchPerson person;
  final OutstandingRegistration registration;
  final int totalRecords;

  SearchPersonEntry(this.person, this.registration, this.totalRecords);
}

class ListUsersBloc implements Bloc {
  String? search;
  final ManagementRepositoryClientBloc mrClient;
  PersonServiceApi _personServiceApi;


  late StreamSubscription<String?>? _globalRefresherSubscriber;

  ListUsersBloc(this.search, this.mrClient)
      : _personServiceApi = PersonServiceApi(mrClient.apiClient) {
    _globalRefresherSubscriber = mrClient.streamValley.globalRefresherStream.listen((event) {
      if (mrClient.userIsSuperAdmin) {
        _personServiceApi = PersonServiceApi(mrClient.apiClient);
      }
    });
  }

  Future deletePerson(String personId, bool includeGroups) {
    return mrClient.personServiceApi
        .deletePerson(personId, includeGroups: includeGroups);
  }

  Future<String> resetApiKey(SearchPerson person) async {
    return (await mrClient.personServiceApi.resetSecurityToken(person.id)).token;
  }


  Future<SearchPersonResult> findPeople(pageSize, startAt, filter, sortOrder, personType) async {
    return _personServiceApi.findPeople(
        order: sortOrder,
        filter: filter,
        countGroups: true,
        includeGroups: false,
        includeLastLoggedIn: true,
        includeDeactivated: true,
        pageSize: pageSize,
        startAt: startAt,
        personTypes: [personType]);
  }

  List<SearchPersonEntry> transformPeople(SearchPersonResult data) {
    final results = <SearchPersonEntry>[];

    final hasLocal = mrClient.identityProviders.hasLocal;
    final emptyReg = OutstandingRegistration(expired: false, id: '', token: '');

    for (var person in data.summarisedPeople) {
      final spr = SearchPersonEntry(
          person,
          hasLocal
              ? data.outstandingRegistrations.firstWhere(
                  (element) => element.id == person.id,
              orElse: () => emptyReg)
              : emptyReg, data.max);

      results.add(spr);
    }
    return results;
  }

  List<SearchPerson> transformAdminApiKeys(SearchPersonResult data) {
    final results = <SearchPerson>[];

    for (var person in data.summarisedPeople) {
      final spr = person;

      results.add(spr);
    }
   return results;
  }

  Future<Person> getPerson(String id) async {
    return await _personServiceApi.getPerson(id, includeGroups: true);
  }

  @override
  void dispose() {
    // cancel subs first
    _globalRefresherSubscriber?.cancel();
    _globalRefresherSubscriber = null;
  }

  Future<void> activatePerson(String id) async {
    var person = await getPerson(id);
    var up = UpdatePerson(version: person.version!, unarchive: true);
    return await _personServiceApi.updatePersonV2(id, up);
  }
}
