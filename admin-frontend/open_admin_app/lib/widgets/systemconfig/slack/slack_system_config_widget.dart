import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_outlined_widget.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_encryptable_map.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_encrypted_text_field.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_text_field.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';

class SlackSystemConfigWidget extends StatefulWidget {
  final List<SystemConfig> knownConfigs;

  const SlackSystemConfigWidget({super.key, required this.knownConfigs});

  @override
  State<StatefulWidget> createState() {
    return SlackSystemConfigState();
  }
}

class _SystemConfig {
  final String key;
  final dynamic value;

  _SystemConfig(this.key, this.value);
}

class SlackSystemConfigState extends State<SlackSystemConfigWidget> {
  late SystemConfigBloc configBloc;
  final SystemConfigEncryptionController _deliveryHeadersController = SystemConfigEncryptionController();
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final Map<String, SystemConfig> settings = {};
  final List<_SystemConfig> unchangedSettings = [];
  bool _loading = false;

  @override
  Widget build(BuildContext context) {
    final header = Padding(
      padding: const EdgeInsets.all(8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: const FHExternalLinkWidget(
              tooltipMessage: "View documentation",
              link: "https://docs.featurehub.io/featurehub/latest/slack.html",
              icon: Icon(Icons.arrow_outward_outlined),
              label: 'Slack Integration Documentation',
            ),
          )
        ],
      ),
    );

    if (settings.isEmpty || _loading) {
      return Column(
        children: [header, FHLoadingIndicator()],
      );
    }

    try {
      // enabled existing is a constant if this widget is even available
      final enabled = settings['slack.enabled']!;
      // as is the bearer token, but it might be encrypted
      final bearer = settings['slack.bearerToken']!;
      final defaultChannel = settings['slack.defaultChannel']!;

      return Container(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            header,
            Padding(
              padding: const EdgeInsets.only(left: 8.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Text('Enable Slack'),
                  Checkbox(
                      value: enabled.value,
                      onChanged: (val) {
                        setState(() {
                          enabled.value = val;
                        });
                      }),
                ],
              ),
            ),
            Form(
                key: _formKey,
                child: Padding(
                  padding: const EdgeInsets.only(left: 8.0, right: 8.0),
                  child: Card(
                    child: Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                          child: const FHExternalLinkOutlinedWidget(
                              label: 'Connect FeatureHub to Slack',
                              tooltipMessage:
                                  'Install FeatureHub Bot app to your Slack workspace',
                              link:
                                  'https://api.slack.com/apps?new_app=1&manifest_yaml=display_information%3A%0A%20%20name%3A%20FeatureHub%0A%20%20description%3A%20FeatureHub%20Notifications%20Bot%0A%20%20background_color%3A%20%22%23536dfe%22%0A%20%20long_description%3A%20Receive%20notifications%20from%20the%20FeatureHub%20Bot.%20Notifications%20include%20features%20and%20feature%20values%20updates%2C%20strategy%20updates%20and%20other%20feature%20settings.%20For%20details%2C%20please%20view%20our%20documentation%20on%20https%3A%2F%2Fdocs.featurehub.io%0Afeatures%3A%0A%20%20bot_user%3A%0A%20%20%20%20display_name%3A%20featurehub%0A%20%20%20%20always_online%3A%20true%0Aoauth_config%3A%0A%20%20scopes%3A%0A%20%20%20%20bot%3A%0A%20%20%20%20%20%20-%20chat%3Awrite%0Asettings%3A%0A%20%20org_deploy_enabled%3A%20false%0A%20%20socket_mode_enabled%3A%20false%0A%20%20token_rotation_enabled%3A%20false%0A',
                              icon: Icon(Icons.arrow_outward_outlined)),
                        ),
                        Padding(
                          padding: const EdgeInsets.only(top: 16.0),
                          child: SystemConfigEncryptableTextField(
                            field: bearer,
                            decoration: const InputDecoration(
                              border: OutlineInputBorder(),
                              hintText:
                                  'e.g. xoxb-1182138673840-5153275439522-sYLjc5KVxFaLrr2wY9fh8jd',
                              labelText: 'Slack Bot User OAuth Token',
                            ),
                            validator: (val) {
                              if (enabled.value == true &&
                                  (val == null || val.trim().isEmpty)) {
                                return 'Cannot enable Slack if the bearer token is empty';
                              }

                              return null;
                            },
                          ),
                        ),
                        Padding(
                          padding: const EdgeInsets.only(top: 16.0),
                          child: SystemConfigTextField(
                            field: defaultChannel,
                            decoration: const InputDecoration(
                              border: OutlineInputBorder(),
                              hintText: 'e.g. C0150T7AF25',
                              labelText: 'Default Slack channel ID',
                            ),
                          ),
                        ),
                        if (settings['slack.delivery.url'] != null)
                          externalDelivery(),
                      ],
                    ),
                  ),
                )),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Row(
                children: [
                  FilledButton(
                      onPressed: () async {
                        if (_formKey.currentState!.validate()) {
                          _deliveryHeadersController.submit();
                          _formKey.currentState!.save();
                          await _save();
                        }
                      },
                      child: const Text('Save')),
                ],
              ),
            )
          ],
        ),
      );
    } catch (e, s) {
      fhosLogger.severe("failed ${e}: ${s}");
      return SizedBox.shrink();
    }
  }

  Widget externalDelivery() {
    final deliveryUrlValidator = (settings['slack.delivery.prefixes'] == null) ? <String>[] : (settings['slack.delivery.prefixes']!.value as List<dynamic>).map((e) => e.toString()).toList();
    final deliveryUrlHelp = deliveryUrlValidator.join(", ");

    return Card(
      child:
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(children: [
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: const Text('If your Slack delivery is offloaded to an external application, please specify the details here.'),
            ),
            if (settings['slack.delivery.url'] != null)
              Padding(
                padding: const EdgeInsets.only(top: 16.0),
                child: SystemConfigTextField(
                  field: settings['slack.delivery.url']!,
                  decoration: InputDecoration(
                    border: OutlineInputBorder(),
                    hintText:
                    'e.g. http://slack-service.cluster.local',
                    labelText:
                    'External Slack message delivery service (optional, valid prefixes ${deliveryUrlHelp})',
                  ),
                  validator: (String? val) {
                    if (val == null || val.isEmpty) return null;

                    if (deliveryUrlValidator.none((e) => val.startsWith(e) == true))
                      return 'You must choose a valid url prefix';

                    return null;
                  } ,),
              ),
            if (settings['slack.delivery.headers'] != null)
              Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: SystemConfigEncryptableMapWidget(field: settings['slack.delivery.headers']!,
                    controller: _deliveryHeadersController,
                    keyHeaderName: 'Header', valueHeaderName: 'Value',
                    defaultNewKeyName: 'X-Header', defaultNewValueName: 'value'),
              )
          ],),
        )
      ,
    );
  }

  Future _save() async {
    final changed = <SystemConfig>[];
    settings.forEach((key, value) {
      final original = unchangedSettings.firstWhere((e) => e.key == key);
      fhosLogger.info("comparing ${original.value} to ${value}");
      if (original.value != value.value) {
        fhosLogger.info("changed");
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

        await _refresh(wrapState: false);
      } catch (e, s) {
        configBloc.mrClient.addError(FHError('Unable to save Slack updates', exception: e, stackTrace: s));
      }
    }
  }

  Future _refresh({bool wrapState = true}) async {
    if (_loading) return;

    _loading = true;

    try {
      configBloc = BlocProvider.of(context);

      final configs = await configBloc.systemConfigServiceApi
          .getSystemConfig(filters: ['slack.']);

      // when you save, its already in a setState
      if (wrapState) {
        setState(() => _reset(configs.configs));
      } else {
        _reset(configs.configs);
      }
    } finally {
      _loading = false;
    }
  }

  void _reset(List<SystemConfig> configs) {
    settings.clear();
    configs.forEach((cfg) {
    settings[cfg.key] = cfg;
    });
    if (settings['slack.delivery.headers'] != null) {
      _deliveryHeadersController.updateField(settings['slack.delivery.headers']!);
    }
    unchangedSettings.clear();
    unchangedSettings.addAll(configs.map((e) => _SystemConfig(e.key, e.value)));
  }

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _refresh();
  }
}




