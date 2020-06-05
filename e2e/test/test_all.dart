import 'package:ogurets/ogurets.dart';

import 'steps/AdminGroupStepdefs.dart';
import 'steps/ApplicationStepdefs.dart';
import 'steps/EnvironmentStepdefs.dart';
import 'steps/FeatureStepdefs.dart';
import 'steps/Hooks.dart' as Hooks;

import 'steps/PersonStepdefs.dart' as PersonStepdefs;
import 'steps/SystemStepdefs.dart' as SystemStepdefs;
import 'steps/PortfolioStepdefs.dart' as PortfolioStepdefs;
import 'steps/UserSystemStepdefs.dart' as UserSystemStepdefs;
import 'steps/AdminPersonStepdefs.dart' as AdminPersonStepdefs;
import 'steps/AdminPortfolioStepdefs.dart' as AdminPortfolioStepdefs;

/// this file is manually maintained, if you add new stepdefs, you must add them to this list.
void main(args) async {
  var def = new OguretsOpts()
    ..feature('test/features')
    ..step(Hooks.Hooks)..step(PersonStepdefs.PersonStepdefs)..step(
      SystemStepdefs.SystemStepdefs)..step(
      PortfolioStepdefs.PortfolioStepdefs)..step(
      UserSystemStepdefs.UserSystemStepdefs)..step(
      AdminPersonStepdefs.AdminPersonStepdefs)..step(
      AdminPortfolioStepdefs.AdminPortfolioStepdefs)..step(AdminGroupStepdefs)
      ..step(FeatureStepdefs)
      ..step(EnvironmentStepdefs)
      ..step(ApplicationStepdefs)
  ;

  await def.run();
}
