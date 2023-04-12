import 'dart:convert';

import 'package:args/command_runner.dart';
import 'package:fh_cli/feature-base.dart';
import 'package:fh_cli/feature-strategy.dart';
import 'package:fh_cli/login.dart';

class FeatureListCommand extends PortfolioApplicationCommand {
  final String name = "list";
  final String description = "List feature values";

  FeatureSetCommand() {}

  @override
  Future<void> run() async {
    final login = await (Login(globalResults))
      ..login();

    final ps = await findStructure(login);
    if (ps.application != null) {
      final features = await login.identity.findFeatures(ps.application!.id!);
      print(jsonEncode(features));
    }
  }
}

class FeatureSetCommand extends Command {
  final String name = "set";
  final String description = "Set feature values";

  FeatureSetCommand() {
    argParser.addOption('value', help: 'the value to set', mandatory: false);
  }

  @override
  Future<void> run() async {
    await (Login(globalResults)).login();
  }
}

class FeatureGetCommand extends Command {
  final String name = "get";
  final String description = "Get feature value";
}

class FeatureCommand extends Command {
  final String name = "feature";
  final String description = "Get, Set, Create, Delete features";

  FeatureCommand() {
    argParser.addOption('key',
        help: 'The KEY of the feature to use', mandatory: true);
    argParser.addOption('environment',
        help: 'The name of the environment to use', mandatory: true);
    addSubcommand(FeatureSetCommand());
    addSubcommand(FeatureGetCommand());
    addSubcommand(FeatureListCommand());
    addSubcommand(FeatureGetStrategyCommand());
    addSubcommand(FeatureSetStrategyCommand());
    addSubcommand(FeatureListStrategyCommand());
  }

  @override
  Future<void> run() async {}
}
