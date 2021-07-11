import 'dart:io';

import 'package:args/command_runner.dart';

import 'host.dart';
import 'identity.dart';

class FeatureSetCommand extends Command {
  final String name = "set";
  final String description = "Set feature values";

  FeatureSetCommand() {
    argParser.addOption('value', help: 'the value to set');
  }

  @override
  Future<void> run() async {
    final username = argResults!['username'];
    final password = argResults!['password'];
    final mrHost = argResults!['mr-host'];

    if (username == null || password == null) {
      print('credentials are currently required for each request');
      exit(-1);
    }

    if (mrHost == null) {
      print('please specify the Admin host with --mr-host HOSTNAME');
      exit(-1);
    }

    final host = Host(apiHost: mrHost);

    final identity = Identity(host, username: username, password: password);

    await identity.login();
  }
}

class FeatureCommand extends Command {
  final String name = "feature";
  final String description = "Get, Set, Create, Delete features";

  FeatureCommand() {
    argParser.addOption(
      'key',
      help: 'The KEY of the feature to use',
    );
    argParser.addOption('environment',
        help: 'The name of the environment to use');
    addSubcommand(FeatureSetCommand());
  }
}

main(List<String> passedArgs) async {
  final runner = CommandRunner("clifh", "Command line Feature Hub")
    ..addCommand(FeatureCommand());

  runner.argParser
      .addOption('username', help: 'The email of the user to login as');
  runner.argParser
      .addOption('password', help: 'The password of the user to login as');
  runner.argParser
      .addOption('mr-host', help: 'The address of the server to login to');

  runner.argParser.addOption('portfolio', help: 'portfolio name to find');
  runner.argParser.addOption('application', help: 'application to find');

  runner.run(passedArgs);

  // final featureFilename = args['features-wu'];
  // if (featureFilename != null) {
  //   final featureFile = await File.fromUri(Uri.file(featureFilename));
  //
  //   if (!(await featureFile.exists())) {
  //     print("Upload file ${featureFilename} does not exist");
  //     exit(-1);
  //   }
  //
  //   final features = FeaturesWuCommand(host, await featureFile.readAsString());
  //
  //   final portfolioName = args['portfolio'];
  //   final applicationName = args['application'];
  //   if (portfolioName == null || applicationName == null) {
  //     print("both application and portfolio must be specified");
  //     exit(-1);
  //   }
  //
  //   final portfolio = await identity.findPortfolio(portfolioName);
  //   if (portfolio == null) {
  //     print("Unable to find a portfolio called `${portfolioName}`");
  //     exit(-1);
  //   }
  //
  //   final application =
  //       await identity.findApplication(portfolio.id, applicationName);
  //
  //   if (application == null) {
  //     print(
  //         "Unable to find an application `${applicationName}` in portfolio `${portfolio.name}");
  //     exit(-1);
  //   }
  //
  //   await features.process(portfolio, application);
  // } else {
  //   print("Logged in but nothing to do!");
  // }
}
