import 'package:args/command_runner.dart';
import 'package:fh_cli/login.dart';

class FeatureListStrategyCommand extends Command {
  final String name = "list-strategy";
  final String description = "Set feature rollout strategy values";

  @override
  Future<void> run() async {
    final login = await (Login(globalResults)).login();
  }
}

class FeatureGetStrategyCommand extends Command {
  final String name = "get-strategy";
  final String description = "Set feature rollout strategy values";

  FeatureGetStrategyCommand() {
    argParser.addOption('name', help: 'the strategy name', mandatory: true);
  }

  @override
  Future<void> run() async {
    final login = await (Login(globalResults)).login();
  }
}

class FeatureSetStrategyCommand extends Command {
  final String name = "set-strategy";
  final String description = "Set feature rollout strategy values";

  FeatureSetStrategyCommand() {
    argParser.addOption('value', help: 'the value to set', mandatory: false);
    argParser.addOption('name', help: 'the strategy name', mandatory: true);
  }

  @override
  Future<void> run() async {
    final login = await (Login(globalResults)).login();
  }
}
