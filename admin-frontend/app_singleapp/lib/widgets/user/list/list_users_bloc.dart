import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class ListUsersBloc implements Bloc {
  String search;
  final ManagementRepositoryClientBloc mrClient;
  PersonServiceApi _personServiceApi;

  Stream<List<Person>> get personSearch => _personSearchResultSource.stream;
  final _personSearchResultSource = BehaviorSubject<List<Person>>();

  ListUsersBloc(this.search, this.mrClient) : assert(mrClient != null) {
    _personServiceApi = PersonServiceApi(mrClient.apiClient);
    triggerSearch(search);
  }

  void triggerSearch(String s) async {
    // this should also change the url

    // debounce the search (i.e. if they are still typing, wait)
    final newSearch = s;
    search = s;

    await Timer(Duration(milliseconds: 300), () {
      if (newSearch == search) {
        // hasn't changed
        _requestSearch(); // don't need to await it, async is fine
      }
    });
  }

  Future deletePerson(String personId, bool includeGroups) {
    return mrClient.personServiceApi
        .deletePerson(personId, includeGroups: includeGroups);
  }

  // this really runs the search after we have debounced it
  void _requestSearch() async {
    if (search != null && search.length > 1) {
      // wait for global error handling to wrap this in try/catch
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC, filter: search, includeGroups: true);

      // publish it out...
      _personSearchResultSource.add(data.people);
    } else if (search == null || search.isEmpty) {
      // this should paginate one presumes
      var data = await _personServiceApi.findPeople(
          order: SortOrder.ASC, includeGroups: true);

      // publish it out...
      _personSearchResultSource.add(data.people);
    }
  }

  @override
  void dispose() {
    _personSearchResultSource.close();
  }
}
