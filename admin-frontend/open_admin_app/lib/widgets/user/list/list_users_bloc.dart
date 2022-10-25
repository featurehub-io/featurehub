import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';

class SearchPersonEntry {
  final SearchPerson person;
  final OutstandingRegistration registration;

  SearchPersonEntry(this.person, this.registration);
}

class SearchPersonPagination {
  final int pageSize;
  final int startPos;
  final SearchPersonResult result;

  SearchPersonPagination(this.pageSize, this.startPos, this.result);
}

class ListUsersBloc implements Bloc {
  String? search;
  int pageSize = 5;
  int startPos = 0;

  final ManagementRepositoryClientBloc mrClient;
  PersonServiceApi _personServiceApi;

  bool isPerson;

  final _adminApiKeysSearchResultSource = BehaviorSubject<List<SearchPerson>>();

  Stream<List<SearchPerson>> get adminAPIKeySearch =>
      _adminApiKeysSearchResultSource.stream;

  Stream<List<SearchPersonEntry>> get personSearch =>
      _personSearchResultSource.stream;
  final _personSearchResultSource = BehaviorSubject<List<SearchPersonEntry>>();
  final _searchResultSource = BehaviorSubject<SearchPersonPagination>();

  Stream<SearchPersonPagination> get searchResult => _searchResultSource.stream;

  late StreamSubscription<String?>? _globalRefresherSubscriber;

  ListUsersBloc(this.search, this.mrClient, this.isPerson)
      : _personServiceApi = PersonServiceApi(mrClient.apiClient) {
    _globalRefresherSubscriber =
        mrClient.streamValley.globalRefresherStream.listen((event) {
      if (mrClient.userIsSuperAdmin) {
        _personServiceApi = PersonServiceApi(mrClient.apiClient);
        triggerSearch(search);
      }
    });
  }

  void triggerSearch(String? s) async {
    // this should also change the url

    // debounce the search (i.e. if they are still typing, wait)
    final newSearch = s;
    search = s;

    Timer(const Duration(milliseconds: 300), () {
      if (newSearch == search) {
        _requestSearch(isPerson ? PersonType.person : PersonType.serviceAccount);
        // don't need to await it, async is fine
      }
    });
  }

  Future deletePerson(String personId, bool includeGroups) {
    return mrClient.personServiceApi
        .deletePerson(personId, includeGroups: includeGroups);
  }

  Future<String> resetApiKey(SearchPerson person) async {
    return (await mrClient.personServiceApi.resetSecurityToken(person.id))
        .token;
  }

  // this really runs the search after we have debounced it
  void _requestSearch(
    PersonType personType,
  ) async {
    SearchPersonResult data = (search != null && search!.isNotEmpty)
        ?
        // wait for global error handling to wrap this in try/catch
        (await _personServiceApi.findPeople(
            order: SortOrder.ASC,
            filter: search,
            countGroups: true,
            includeGroups: false,
            includeLastLoggedIn: true,
            pageSize: pageSize,
            startAt: startPos,
            personTypes: [personType]))
        : (
            // this should paginate one presumes
            await _personServiceApi.findPeople(
                order: SortOrder.ASC,
                includeGroups: false,
                countGroups: true,
                includeLastLoggedIn: true,
                pageSize: pageSize,
                startAt: startPos,
                personTypes: [personType]));

    _searchResultSource.add(SearchPersonPagination(pageSize, startPos, data));

    // publish it out...
    if (personType == PersonType.person) {
      _transformPeople(data);
    } else {
      _transformAdminApiKeys(data);
    }
  }

  void _reload() {
    _requestSearch(isPerson ? PersonType.person : PersonType.serviceAccount);
  }

  void next() {
    startPos += pageSize;
    _reload();
  }

  void prev() {
    startPos -= pageSize;
    _reload();
  }

  void last() {
    if (_searchResultSource.hasValue) {
      // if the last page only has e.g. 1 item, we need to set the startPos to bring back that 1 item,
      // otherwise the pages are all muffed up
      final lastPageSize = _searchResultSource.value!.result.max % pageSize;
      startPos = _searchResultSource.value!.result.max - lastPageSize;
      _reload();
    }
  }

  void first() {
    startPos = 0;
    _reload();
  }

  void page(int count) {
    startPos = (count - 1) * pageSize;
    _reload();
  }

  void _transformPeople(SearchPersonResult data) {
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
              : emptyReg);

      results.add(spr);
    }

    // publish it out...
    _personSearchResultSource.add(results);
  }

  void _transformAdminApiKeys(SearchPersonResult data) {
    final results = <SearchPerson>[];
    // publish it out...

    for (var person in data.summarisedPeople) {
      final spr = person;

      results.add(spr);
    }
    _adminApiKeysSearchResultSource.add(results);
  }

  Future<Person> getPerson(String id) async {
    return await _personServiceApi.getPerson(id, includeGroups: true);
  }

  @override
  void dispose() {
    // cancel subs first
    _globalRefresherSubscriber?.cancel();
    _globalRefresherSubscriber = null;
    _personSearchResultSource.close();
    _adminApiKeysSearchResultSource.close();
    _searchResultSource.close();
  }
}

class ListPersonBloc extends ListUsersBloc {
  ListPersonBloc(String? search, ManagementRepositoryClientBloc mrClient)
      : super(search, mrClient, true);

}

class ListAdminServiceAccount extends ListUsersBloc {
  ListAdminServiceAccount(
      String? search, ManagementRepositoryClientBloc mrClient)
      : super(search, mrClient, false);
}
