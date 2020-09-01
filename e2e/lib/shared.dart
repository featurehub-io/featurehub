import 'package:mrapi/api.dart';

class Shared {
  Map<String, Object> _data = {};

  RegistrationUrl registrationUrl;

  Portfolio get portfolio => _data['portfolio'] as Portfolio;

  set serviceAccount(ServiceAccount serviceAccount) =>
      _data['sa'] = serviceAccount;
  ServiceAccount get serviceAccount => _data['sa'] as ServiceAccount;
  set portfolio(Portfolio p) => _data['portfolio'] = p;

  Person get person => _data['person'] as Person;
  set person(Person p) => _data['person'] = p;

  Group get group => _data['group'] as Group;
  set group(Group g) => _data['group'] = g;

  TokenizedPerson get tokenizedPerson => _data['token'] as TokenizedPerson;
  set tokenizedPerson(TokenizedPerson p) => _data['token'] = p;

  Application get application => _data['application'] as Application;
  set application(Application a) => _data['application'] = a;

  Environment get environment => _data['env'] as Environment;
  set environment(Environment e) => _data['env'] = e;

  FeatureValue featureValue;

  Feature feature;

  Group portfolioAdminGroup;
}
