import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';

class SearchPersonEntry {
  final Person person;
  final OutstandingRegistration registration;

  SearchPersonEntry(this.person, this.registration);
}

class ListUsersBloc implements Bloc {
  String? search;
  final ManagementRepositoryClientBloc mrClient;
  final PersonServiceApi _personServiceApi;

  bool isPerson;

  final _adminApiKeysSearchResultSource = BehaviorSubject<List<Person>>.seeded([]);
  Stream<List<Person>> get adminAPIKeySearch =>
      _adminApiKeysSearchResultSource.stream;
  Stream<List<SearchPersonEntry>> get personSearch =>
      _personSearchResultSource.stream;
  final _personSearchResultSource =
      BehaviorSubject<List<SearchPersonEntry>>.seeded([]);

  ListUsersBloc(this.search, this.mrClient, this.isPerson)
      : _personServiceApi = PersonServiceApi(mrClient.apiClient) {
    triggerSearch(search, true);
    triggerSearch(search, false);
  }

  void triggerSearch(String? s, bool isPerson) async {
    // this should also change the url

    // debounce the search (i.e. if they are still typing, wait)
    final newSearch = s;
    search = s;

    Timer(const Duration(milliseconds: 300), () {
      if (newSearch == search) {
        // hasn't changed
        if(isPerson) {
          _requestSearch();
        }
        else {
          _requestAdminApiKeysSearch();
        }
     // don't need to await it, async is fine
      }
    });
  }

  Future deletePerson(String personId, bool includeGroups) {
    return mrClient.personServiceApi
        .deletePerson(personId, includeGroups: includeGroups);
  }

  // this really runs the search after we have debounced it
  void _requestSearch() async {
    if (search != null && search!.length > 1) {
      // wait for global error handling to wrap this in try/catch
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC,
          filter: search,
          includeGroups: true,
          includeLastLoggedIn: true,
          personTypes: [PersonType.person]
      );

      // publish it out...
      _transformPeople(data);
    } else if (search == null || search!.isEmpty) {
      // this should paginate one presumes
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC,
          includeGroups: true,
          includeLastLoggedIn: true,
          personTypes: [PersonType.person]);
      _transformPeople(data);
    }
  }

  void _requestAdminApiKeysSearch() async {
    if (search != null && search!.length > 1) {
      // wait for global error handling to wrap this in try/catch
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC,
          filter: search,
          includeGroups: true,
          personTypes: [PersonType.serviceAccount]
      );

      // publish it out...
      _transformAdminApiKeys(data);
    } else if (search == null || search!.isEmpty) {
      // this should paginate one presumes
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC,
          includeGroups: true,
          personTypes: [PersonType.serviceAccount]);
      _transformAdminApiKeys(data);
    }
  }

  void _transformPeople(SearchPersonResult data) {
    final results = <SearchPersonEntry>[];

    final hasLocal = mrClient.identityProviders.hasLocal;
    final emptyReg = OutstandingRegistration(expired: false, id: '', token: '');

    for (var person in data.people) {
      final spr = SearchPersonEntry(
          person,
          hasLocal
              ? data.outstandingRegistrations.firstWhere(
                  (element) => element.id == person.id!.id,
                  orElse: () => emptyReg)
              : emptyReg);

      results.add(spr);
    }

    // publish it out...
    _personSearchResultSource.add(results);
  }

  void _transformAdminApiKeys(SearchPersonResult data) {
    final results = <Person>[];
    // publish it out...

    for (var person in data.people) {
      final spr = person;

      results.add(spr);
    }
    _adminApiKeysSearchResultSource.add(results);
  }

  @override
  void dispose() {
    _personSearchResultSource.close();
  }
}
