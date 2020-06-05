
import 'package:mrapi/api.dart';

class Shared {
  Map<String, Object> _data = {};

  RegistrationUrl registrationUrl;

  Portfolio get portfolio => _data["portfolio"] as Portfolio;
  set portfolio(Portfolio p) => _data["portfolio"] = p;

  Person get person => _data["person"] as Person;
  set person(Person p) => _data["person"] = p;

  Group get group => _data["group"] as Group;
  set group(Group g) => _data["group"] = g;

  Application get application => _data["application"] as Application;
  set application(Application a) => _data["application"] = a;

  Environment get environment => _data["env"]  as Environment;
  set environment(Environment e) => _data["env"] = e;

  Group portfolioAdminGroup;
}
