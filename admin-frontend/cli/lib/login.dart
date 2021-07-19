import 'dart:io';

import 'package:args/args.dart';
import 'package:yaml/yaml.dart';

import 'host.dart';
import 'identity.dart';

final defaultConfigFileName =
    (Platform.environment['HOME'] ?? '') + '/.featurehub/cli-config.yaml';
const localConfigFileName = 'cli-config.yaml';

class Login {
  final ArgResults? argResults;
  late Host host;
  late Identity identity;

  Login(this.argResults);

  Future<void> login() async {
    print("options are ${argResults!.options}");
    final config =
        argResults!.wasParsed('config') ? argResults!['config'] : null;

    if (!argResults!.wasParsed('username') ||
        !argResults!.wasParsed('password') ||
        !argResults!.wasParsed('mr-host')) {
      if (config == null) {
        if (await File(defaultConfigFileName).exists()) {
          await _loadHostAndIdentityFromConfig(defaultConfigFileName);
        } else if (await File(localConfigFileName).exists()) {
          await _loadHostAndIdentityFromConfig(localConfigFileName);
        } else {
          throw Exception(
              'Unable to determine identity, either pass config file, create one in ${defaultConfigFileName} or ${localConfigFileName}, or pass on command line');
        }
      } else {
        if (await File(config).exists()) {
          await _loadHostAndIdentityFromConfig(config);
        } else {
          throw Exception(
              'No such file ${config} exists that holds necessary config for authentication');
        }
      }
    } else {
      final username = argResults!['username'];
      final password = argResults!['password'];
      final mrHost = argResults!['mr-host'];

      host = Host(apiHost: mrHost);

      identity = Identity(host, username: username, password: password);
    }

    await identity.login();

    if (argResults!.wasParsed('write-config')) {
      await _writeConfig(identity.username, identity.password, host.apiHost,
          config == null ? defaultConfigFileName : config!);
    }
  }

  Future<void> _loadHostAndIdentityFromConfig(String configFile) async {
    final doc = loadYaml(await File(configFile).readAsString());

    if (doc['username'] != null &&
        doc['password'] != null &&
        doc['mr-host'] != null) {
      host = Host(apiHost: doc['mr-host']);

      identity =
          Identity(host, username: doc['username'], password: doc['password']);

      print("iden");
    } else {
      throw Exception(
          'Config file $configFile is malformed, it needs\nusername: <username>\npassword: <password>\nmr-host: <admin-host-url>\ndefined.');
    }
  }

  Future<void> _writeConfig(String username, String password, String hostname,
      String configFileName) async {
    print("writing config to $configFileName");

    final f = await File(configFileName);
    await f.parent.create(recursive: true);
    await f.writeAsString('''
username: $username
password: $password
mr-host: $hostname
''', flush: true);
    final exists = await f.exists();
    print("file exists? $exists ");
  }
}
