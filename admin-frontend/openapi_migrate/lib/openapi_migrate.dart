// @dart=v2.9

import 'dart:io';

import 'package:args/args.dart';
import 'package:codemod/codemod.dart';
import 'package:openapi_migrate/openapi_suggestor.dart';
import 'package:yaml/yaml.dart';

ArgResults _parseArguments(args) {
  var argParser = ArgParser();

  argParser.addOption('openapi');
  return argParser.parse(args);
}

main(passed_args) async {
  ArgResults args = _parseArguments(passed_args);

  String openapiFile =
      args['openapi'] ?? '../admin-frontend/app_mr_layer/final.yaml';
  if (openapiFile == null) {
    print(
        "You must specify the location of the openapi file with --openapi <filename>");
    return;
  }

  final f = await File(openapiFile);
  if (!(await f.exists())) {
    print("No file called ${openapiFile} exists");
    return;
  }

  String contents = await f.readAsString();

  final api = loadYaml(contents);

  final schema = api['components']['schemas'] as YamlMap;

  openapiClasses = schema.keys.map((e) => e.toString()).toList();

  await runInteractiveCodemod(
      [
        'test/steps/StrategiesStepdefs.dart',
        'test/steps/UserStateStepdefs.dart',
        'test/steps/UserSystemStepdefs.dart',
      ],
      OpenPatchSuggestor(),
      args: []);
}
