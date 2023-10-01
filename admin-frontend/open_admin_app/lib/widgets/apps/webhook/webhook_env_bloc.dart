import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';


class EnvironmentAndWebhookType {
  final WebhookTypeDetail? type;
  final Environment? environment;

  EnvironmentAndWebhookType(this.type, this.environment);

  EnvironmentAndWebhookType fromType(WebhookTypeDetail? type) {
    return EnvironmentAndWebhookType(type, environment);
  }

  EnvironmentAndWebhookType fromEnv(Environment? env) {
    return EnvironmentAndWebhookType(type, env);
  }
}

class WebhookEnvironmentBloc extends Bloc {
  final ManagementRepositoryClientBloc mrBloc;
  final List<Environment> environments;
  final String appId;

  late BehaviorSubject<EnvironmentAndWebhookType> _environmentSource;
  Stream<EnvironmentAndWebhookType> get environmentStream => _environmentSource.stream;

  final _viewWebookSource = BehaviorSubject<WebhookDetail?>();
  Stream<WebhookDetail?> get viewWebhookStream => _viewWebookSource.stream;

  WebhookEnvironmentBloc(this.mrBloc, this.environments, this.appId) {
    if (environments.isNotEmpty) {
      _environmentSource = BehaviorSubject<EnvironmentAndWebhookType>.seeded(EnvironmentAndWebhookType(mrBloc.streamValley.firstWebhookType, environments[0]));
      // this will force the underlying set of data to update
      current = environments[0];
    } else {
      _environmentSource = BehaviorSubject<EnvironmentAndWebhookType>.seeded(EnvironmentAndWebhookType(mrBloc.streamValley.firstWebhookType, null));
    }
  }

  @override
  void dispose() {
    _environmentSource.close();
  }

  Environment? get current => _environmentSource.value.environment;
  WebhookTypeDetail? get currentWebhookType => _environmentSource.value.type;

  EnvironmentAndWebhookType get _currentSource => _environmentSource.value;

  // we can't use the list from the ManageAppBloc as it doesn't contain the environmentInfo details
  // so we need to grab it and hold onto and use that one
  set current(Environment? e) {
    if (e != null) {
      mrBloc.environmentServiceApi.getEnvironment(e.id, includeDetails: true)
          .then((env) {
        _environmentSource.add(_currentSource.fromEnv(env));
      }
      );
    } else {
      _environmentSource.add(_currentSource.fromEnv(null));
    }
  }

  set webhookType(WebhookTypeDetail? type) {
    _environmentSource.add(_currentSource.fromType(type));
  }

  set viewItem(String? viewItemId) {
    if (viewItemId == null && current != null) {
      _viewWebookSource.add(null);
    } else {
      mrBloc.webhookServiceApi.getWebhookDetails(current!.id, viewItemId!)
          .then((webhook) => _viewWebookSource.add(webhook));
    }
  }

  Future<void> updateEnvironment(Environment env) async {
    // TODO check this
    //     final envData = await mrBloc.environmentServiceApi.updateEnvironmentOnApplication(appId,
    //         UpdateEnvironmentV2(id: env.id, version: env.version, environmentInfo: env.environmentInfo), includeDetails: true);
    final envData = await mrBloc.environment2ServiceApi.updateEnvironmentV2(env.id!,
        UpdateEnvironment(version: env.version!, webhookEnvironmentInfo: env.webhookEnvironmentInfo), includeDetails: true);
    _environmentSource.add(_currentSource.fromEnv(envData));
  }

  Future<void> sendWebhookCheck(WebhookCheck webhookCheck) async {
    await mrBloc.webhookServiceApi.testWebhook(webhookCheck);
  }

  Future<void> decryptEncryptedFields(Environment e) async {
    // final envData = await mrBloc.environment2ServiceApi.
    final envData = await mrBloc.environmentServiceApi.getEnvironment(e.id!, includeDetails: true, decryptWebhookDetails: true);
    _environmentSource.add(_currentSource.fromEnv(envData));

  }
}
