

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';

class LocalSystemConfig {
  final String key;
  final dynamic value;

  LocalSystemConfig(this.key, this.value);
}

mixin SystemConfigMixin<T extends StatefulWidget> on State<T> {
  late SystemConfigBloc configBloc;
  final Map<String, SystemConfig> settings = {};
  final List<LocalSystemConfig> unchangedSettings = [];
  bool loading = false;

  List<String> get filters => [];

  Future save() async {
    final changed = <SystemConfig>[];
    settings.forEach((key, value) {
      final original = unchangedSettings.firstWhere((e) => e.key == key);
      if (original.value != value.value) {
        changed.add(value);
      }
    });

    if (changed.isNotEmpty) {
      try {
        await configBloc.systemConfigServiceApi.createOrUpdateSystemConfigs(
            UpdatedSystemConfigs(configs: changed.map((e) =>
                UpdatedSystemConfig(key: e.key,
                    version: e.version,
                    value: e.value)).toList()));

        await refresh(wrapState: true);

        configBloc.mrClient.addSnackbar(Text(
            "${namedSection} was successfully updated"));
      } catch (e, s) {
        configBloc.mrClient.addError(FHError('Unable to save Slack updates', exception: e, stackTrace: s));
      }
    } else {
      configBloc.mrClient.addSnackbar(Text("No updates for ${namedSection} found"));
    }
  }

  String get namedSection => '';

  Future refresh({bool wrapState = true}) async {
    if (loading) return;

    loading = true;

    try {
      configBloc = BlocProvider.of(context);

      final configs = await configBloc.systemConfigServiceApi
          .getSystemConfig(filters: filters);

      // when you save, its already in a setState
      if (wrapState) {
        setState(() => reset(configs.configs));
      } else {
        reset(configs.configs);
      }
    } finally {
      loading = false;
    }
  }

  void reset(List<SystemConfig> configs) {
    settings.clear();
    configs.forEach((cfg) {
      settings[cfg.key] = cfg;
    });
    stateReset();
    unchangedSettings.clear();
    unchangedSettings.addAll(configs.map((e) => LocalSystemConfig(e.key, e.value)));
  }

  void stateReset() {
    // put override code in here
  }

  @override
  void initState() {
    super.initState();
    refresh();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    refresh();
  }
}
