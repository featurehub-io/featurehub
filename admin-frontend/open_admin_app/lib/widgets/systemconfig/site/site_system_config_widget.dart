import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
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

class SiteSystemConfigState extends State<SiteSystemConfigWidget> with SystemConfigMixin {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  @override
  Widget build(BuildContext context) {
    if (settings.isEmpty || loading) {
      return FHLoadingIndicator();
    }

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
                            decoration: const InputDecoration(
                              border: OutlineInputBorder(),
                              hintText:
                              'https://featurehub.yourorganisation.ai',
                              labelText: "The url of your organisation's Featurehub Website",
                            ),
                            validator: (val) {
                              if ((val == null || val.trim().isEmpty)) {
                                return 'You cannot specify an empty url';
                              }

                              if (val.startsWith('http://') || val.startsWith('https://')) {
                                return null;
                              }

                              return 'You must specify a valid url for your site';
                            },
                          ),
                        ),
                      if (robotsEnabled != null)
                        Padding(
                          padding: const EdgeInsets.only(left: 8.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.start,
                            children: [
                              const Text('Allow search robots to index'),
                              Checkbox(
                                  value: robotsEnabled.value == true ? true : false,
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
                              const Text('Redirect traffic with bad Hosts header'),
                              Checkbox(
                                  value: defaultChannel.value == true ? true : false,
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
                    child: const Text('Save')),
              ],
            ),
          )
        ],
      );
    } catch (e, s) {
      fhosLogger.severe("failed ${e}: ${s}");
      return SizedBox.shrink();
    }
  }

  @override
  List<String> get filters => ['site.'];

  @override
  String get namedSection => 'Site';
}




