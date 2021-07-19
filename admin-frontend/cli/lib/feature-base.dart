import 'package:args/command_runner.dart';
import 'package:fh_cli/login.dart';
import 'package:mrapi/api.dart';

class PortfolioStructure {
  Portfolio? portfolio;
  Application? application;
  Environment? environment;
}

abstract class PortfolioApplicationCommand extends Command {
  Future<PortfolioStructure> findStructure(Login login) async {
    PortfolioStructure ps = PortfolioStructure();

    if (globalResults!.wasParsed('portfolio') &&
        globalResults!.wasParsed('application')) {
      ps.portfolio =
          await login.identity.findPortfolio(globalResults!['portfolio']);

      if (ps.portfolio == null) {
        print('error: unknown portfolio');
      } else {
        ps.application = await login.identity
            .findApplication(ps.portfolio!.id!, globalResults!['application']);

        if (ps.application == null) {
          print('error: unknown application');
        }
      }
    } else {
      print("error: must specify portfolio & application for this command");
    }

    return ps;
  }
}
