import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_mixin.dart';
import 'package:open_admin_app/widgets/systemconfig/system_config_text_field.dart';

class SiteSystemConfigWidget extends StatefulWidget {
  final List<SystemConfig> knownConfigs;

  const SiteSystemConfigWidget({super.key, required this.knownConfigs});

  @override
  State<StatefulWidget> createState() {
    return SiteSystemConfigState();
  }
}

class SiteSystemConfigState extends State<SiteSystemConfigWidget>
    with SystemConfigMixin {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  @override
  Widget build(BuildContext context) {
    if (settings.isEmpty || loading) {
      return const FHLoadingIndicator();
    }

    final l10n = AppLocalizations.of(context)!;

    try {
      // enabled existing is a constant if this widget is even available
      final url = settings['site.url'];
      // as is the bearer token, but it might be encrypted
      final robotsEnabled = settings['site.enabledRobots'];
      final defaultChannel = settings['site.redirectInvalidHosts'];

      return Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Form(
              key: _formKey,
              child: Padding(
                padding: const EdgeInsets.only(left: 8.0, right: 8.0),
                child: Card(
                  child: Column(
                    children: [
                      if (url != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 16.0),
                          child: SystemConfigTextField(
                            field: url,
                            decoration: InputDecoration(
                              border: const OutlineInputBorder(),
                              hintText:
                                  'https://featurehub.yourorganisation.ai',
                              labelText: l10n.siteUrlLabel,
                            ),
                            validator: (val) {
                              if ((val == null || val.trim().isEmpty)) {
                                return l10n.siteUrlEmptyError;
                              }

                              if (val.startsWith('http://') ||
                                  val.startsWith('https://')) {
                                return null;
                              }

                              return l10n.siteUrlInvalidError;
                            },
                          ),
                        ),
                      if (robotsEnabled != null)
                        Padding(
                          padding: const EdgeInsets.only(left: 8.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.start,
                            children: [
                              Text(l10n.allowSearchRobots),
                              Checkbox(
                                  value: robotsEnabled.value == true
                                      ? true
                                      : false,
                                  onChanged: (val) {
                                    setState(() {
                                      robotsEnabled.value = val;
                                    });
                                  }),
                            ],
                          ),
                        ),
                      if (defaultChannel != null)
                        Padding(
                          padding: const EdgeInsets.only(left: 8.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.start,
                            children: [
                              Text(l10n.redirectBadHostsHeader),
                              Checkbox(
                                  value: defaultChannel.value == true
                                      ? true
                                      : false,
                                  onChanged: (val) {
                                    setState(() {
                                      defaultChannel.value = val;
                                    });
                                  }),
                            ],
                          ),
                        ),
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
                        _formKey.currentState!.save();
                        await save();
                      } else {
                        fhosLogger.info("not valid");
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

  @override
  List<String> get filters => ['site.'];

  @override
  String get namedSection => 'Site';
}
