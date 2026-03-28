import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_outlined_widget.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_encryptable_map.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_encrypted_text_field.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_mixin.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_text_field.dart';
import 'package:universal_html/html.dart';

class SlackSystemConfigWidget extends StatefulWidget {
  final List<SystemConfig> knownConfigs;

  const SlackSystemConfigWidget({super.key, required this.knownConfigs});

  @override
  State<StatefulWidget> createState() {
    return SlackSystemConfigState();
  }
}

class SlackSystemConfigState extends State<SlackSystemConfigWidget>
    with SystemConfigMixin {
  final SystemConfigEncryptionController _deliveryHeadersController =
      SystemConfigEncryptionController();
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  static const _kFontFam = 'FlutterIcon';
  static const String? _kFontPkg = null;

  static const IconData slack =
      IconData(0xf198, fontFamily: _kFontFam, fontPackage: _kFontPkg);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    final header = Padding(
      padding: const EdgeInsets.all(8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          FHExternalLinkWidget(
            tooltipMessage: l10n.viewDocumentation,
            link:
                "https://docs.featurehub.io/featurehub/latest/slack-integration.html",
            icon: const Icon(Icons.arrow_outward_outlined),
            label: l10n.slackIntegrationDocumentation,
          )
        ],
      ),
    );

    if (settings.isEmpty || loading) {
      return Column(
        children: [header, const FHLoadingIndicator()],
      );
    }

    try {
      // enabled existing is a constant if this widget is even available
      final enabled = settings['slack.enabled']!;
      // as is the bearer token, but it might be encrypted
      final bearer = settings['slack.bearerToken']!;
      final defaultChannel = settings['slack.defaultChannel']!;
      final knownSiteUrl = settings['knownsite.url'];

      return Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          header,
          if (bearer.value != null)
            Padding(
              padding: const EdgeInsets.only(left: 16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Text(l10n.enableSlack),
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
                child: Column(
                  children: [
                    if (knownSiteUrl == null &&
                        (bearer.value == null ||
                            bearer.value?.toString().isEmpty == true))
                      Padding(
                        padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                        child: FHExternalLinkOutlinedWidget(
                            label: l10n.connectFeatureHubToSlack,
                            tooltipMessage: l10n.installFeatureHubBot,
                            link:
                                'https://api.slack.com/apps?new_app=1&manifest_yaml=display_information%3A%0A%20%20name%3A%20FeatureHub%0A%20%20description%3A%20FeatureHub%20Notifications%20Bot%0A%20%20background_color%3A%20%22%23536dfe%22%0A%20%20long_description%3A%20Receive%20notifications%20from%20the%20FeatureHub%20Bot.%20Notifications%20include%20features%20and%20feature%20values%20updates%2C%20strategy%20updates%20and%20other%20feature%20settings.%20For%20details%2C%20please%20view%20our%20documentation%20on%20https%3A%2F%2Fdocs.featurehub.io%0Afeatures%3A%0A%20%20bot_user%3A%0A%20%20%20%20display_name%3A%20featurehub%0A%20%20%20%20always_online%3A%20true%0Aoauth_config%3A%0A%20%20scopes%3A%0A%20%20%20%20bot%3A%0A%20%20%20%20%20%20-%20chat%3Awrite%0Asettings%3A%0A%20%20org_deploy_enabled%3A%20false%0A%20%20socket_mode_enabled%3A%20false%0A%20%20token_rotation_enabled%3A%20false%0A',
                            icon: const Icon(Icons.arrow_outward_outlined)),
                      ),
                    if (knownSiteUrl != null &&
                        (bearer.value == null ||
                            bearer.value?.toString().isEmpty == true))
                      Padding(
                        padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                        child: Row(
                          children: [
                            FilledButton.icon(
                              icon: const Icon(slack),
                              onPressed: () async {
                                final url =
                                    await configBloc.knownSiteRedirectUrl(
                                        '/mr-api/slack/oauth2/connect');
                                if (url != null) {
                                  window.location.href = url;
                                }
                              },
                              label: Text(l10n.connectToSlack),
                            ),
                          ],
                        ),
                      ),
                    const SizedBox(
                      height: 16.0,
                    ),
                    if (knownSiteUrl == null)
                      SystemConfigEncryptableTextField(
                        field: bearer,
                        decoration: InputDecoration(
                          border: const OutlineInputBorder(),
                          hintText:
                              'e.g. xoxb-1182138673840-5153275439522-sYLjc5KVxFaLrr2wY9fh8jd',
                          labelText: l10n.slackBotTokenLabel,
                        ),
                        validator: (val) {
                          if (enabled.value == true &&
                              (val == null || val.trim().isEmpty)) {
                            return l10n.slackBotTokenRequired;
                          }

                          return null;
                        },
                      ),
                    const SizedBox(
                      height: 16.0,
                    ),
                    SystemConfigTextField(
                      field: defaultChannel,
                      decoration: InputDecoration(
                        border: const OutlineInputBorder(),
                        hintText: 'e.g. C0150T7AF25',
                        labelText: l10n.defaultSlackChannelIdLabel,
                      ),
                      validator: (v) {
                        if (v?.trim().isEmpty == true) {
                          return l10n.slackChannelIdRequired;
                        }
                        return null;
                      },
                    ),
                    if (settings['slack.delivery.url'] != null)
                      externalDelivery(l10n),
                  ],
                ),
              )),
          Padding(
            padding: const EdgeInsets.only(top: 16.0, left: 8.0, bottom: 16.0),
            child: Row(
              children: [
                FilledButton(
                    onPressed: () async {
                      if (_formKey.currentState!.validate()) {
                        _deliveryHeadersController.submit();
                        _formKey.currentState!.save();
                        await save();
                      }
                    },
                    child: Text(l10n.save)),
              ],
            ),
          )
        ],
      );
    } catch (e, s) {
      fhosLogger.severe("failed $e: $s");
      return const SizedBox.shrink();
    }
  }

  Widget externalDelivery(AppLocalizations l10n) {
    final deliveryUrlValidator = (settings['slack.delivery.prefixes'] == null)
        ? <String>[]
        : (settings['slack.delivery.prefixes']!.value as List<dynamic>)
            .map((e) => e.toString())
            .toList();
    final deliveryUrlHelp = deliveryUrlValidator.join(", ");

    return Column(
      children: [
        const SizedBox(height: 16.0),
        Text(l10n.externalSlackDeliveryMessage),
        if (settings['slack.delivery.url'] != null)
          const SizedBox(height: 16.0),
        SystemConfigTextField(
          field: settings['slack.delivery.url']!,
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: 'e.g. http://slack-service.cluster.local',
            labelText: l10n.externalSlackDeliveryUrlLabel(deliveryUrlHelp),
          ),
          validator: (String? val) {
            if (val == null || val.isEmpty) return null;

            if (deliveryUrlValidator.none((e) => val.startsWith(e) == true)) {
              return l10n.invalidUrlPrefix;
            }

            return null;
          },
        ),
        if (settings['slack.delivery.headers'] != null)
          Padding(
            padding: const EdgeInsets.only(top: 16.0),
            child: SystemConfigEncryptableMapWidget(
                field: settings['slack.delivery.headers']!,
                controller: _deliveryHeadersController,
                keyHeaderName: 'Header',
                valueHeaderName: 'Value',
                defaultNewKeyName: 'X-Header',
                defaultNewValueName: 'value'),
          )
      ],
    );
  }

  @override
  List<String> get filters => ['slack.', 'knownsite.'];

  @override
  String get namedSection => 'Slack Configuration';

  @override
  void stateReset() {
    if (settings['slack.delivery.headers'] != null) {
      _deliveryHeadersController
          .updateField(settings['slack.delivery.headers']!);
    }
  }
}
