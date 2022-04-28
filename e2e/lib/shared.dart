import 'package:mrapi/api.dart';

class Shared {
  Map<String, Object> _data = {};

  String? adminServiceAccountToken;
  RegistrationUrl? registrationUrl;

  Portfolio get portfolio {
    final o = _data['portfolio'];
    if (o == null) {
      throw Exception("portfolio doesn't exist");
    }
    return o as Portfolio;
  }

  set portfolio(Portfolio p) => _data['portfolio'] = p;

  set serviceAccount(ServiceAccount serviceAccount) =>
      _data['sa'] = serviceAccount;
  ServiceAccount get serviceAccount {
    final sa = _data['sa'];
    if (sa == null) {
      throw Exception('service account does not exist');
    }
    return sa as ServiceAccount;
  }

  Person get person {
    final p = _data['person'];
    if (p == null) {
      throw Exception('person does not exist');
    }
    return p as Person;
  }

  set person(Person p) => _data['person'] = p;

  Group get group {
    final g = _data['group'];
    if (g == null) {
      throw Exception('group does not exist');
    }
    return g as Group;
  }

  set group(Group g) => _data['group'] = g;

  TokenizedPerson get tokenizedPerson {
    final t = _data['token'];
    if (t == null) {
      throw Exception('Tokenized person is null');
    }
    return t as TokenizedPerson;
  }

  set tokenizedPerson(TokenizedPerson p) => _data['token'] = p;

  Application get application => _data['application'] as Application;
  set application(Application a) => _data['application'] = a;

  Environment get environment => _data['env'] as Environment;
  set environment(Environment e) => _data['env'] = e;

  FeatureValue? featureValue;

  Feature? feature;

  Group? portfolioAdminGroup;
}
