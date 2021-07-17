import 'dart:convert';

import 'package:args/command_runner.dart';

import 'login.dart';

class PortfolioListCommand extends Command {
  final String name = 'list';
  final String description = 'list available portfolios';

  Future<void> run() async {
    print("running portfolio list");
    final login = Login(globalResults);
    await login.login();

    final portfolios = await login.identity.portfolioServiceApi
        .findPortfolios(includeGroups: false, includeApplications: false);

    print(jsonEncode(portfolios));
  }
}

class PortfolioCommand extends Command {
  final String name = 'portfolio';
  final String description = 'portfolio management';

  PortfolioCommand() {
    addSubcommand(PortfolioListCommand());
  }
}
