import 'dart:convert';
import 'dart:io';

import 'package:args/args.dart';
import 'package:fh_cli/features_wu.dart';

import 'host.dart';
import 'identity.dart';

main(List<String> passedArgs) async {
  var argParser = ArgParser();

  argParser.addOption('username', help: 'The email of the user to login as');
  argParser.addOption('password', help: 'The password of the user to login as');
  argParser.addOption('mr-host', help: 'The address of the server to login to');
  argParser.addOption('features-wu',
      help:
          'A JSON file of features to upload. Requires a portfolio and application as well.');
  argParser.addOption('portfolio', help: 'portfolio name to find');
  argParser.addOption('application', help: 'application to find');

  ArgResults args = argParser.parse(passedArgs);

  final username = args['username'];
  final password = args['password'];
  final mrHost = args['mr-host'];

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

  final featureFilename = args['features'];
  if (featureFilename != null) {
    final featureFile = await File.fromUri(Uri.file(featureFilename));

    if (!(await featureFile.exists())) {
      print("Upload file ${featureFilename} does not exist");
      exit(-1);
    }

    final features =
        FeaturesWu(host, jsonDecode(await featureFile.readAsString()));

    final portfolioName = args['portfolio'];
    final applicationName = args['application'];
    if (portfolioName == null || applicationName == null) {
      print("both application and portfolio must be specified");
      exit(-1);
    }

    final portfolio = await identity.findPortfolio(portfolioName);
    if (portfolio == null) {
      print("Unable to find a portfolio called `${portfolioName}`");
      exit(-1);
    }

    final application =
        await identity.findApplication(portfolio.id, applicationName);

    if (application == null) {
      print(
          "Unable to find an application `${applicationName}` in portfolio `${portfolio.name}");
      exit(-1);
    }

    await features.process(portfolio, application);
  } else {
    print("Logged in but nothing to do!");
  }
}
