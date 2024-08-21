import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/systemconfig/site/site_system_config_widget.dart';
import 'package:open_admin_app/widgets/systemconfig/slack/slack_system_config_widget.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';

class SystemConfigPanel extends StatelessWidget {
  const SystemConfigPanel({super.key});

  @override
  Widget build(BuildContext context) {
    final sysBloc = BlocProvider.of<SystemConfigBloc>(context);

    FHAnalytics.sendScreenView("system-config-panel");

    return StreamBuilder<List<SystemConfig>?>(
        stream: sysBloc.knownConfigStream,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return const FHLoadingError();
          }
          if (!snapshot.hasData) {
            return const FHLoadingIndicator();
          }

          final config = snapshot.data!;

          var isSlackEnabled = config.any((cfg) => cfg.key == 'slack.enabled');

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const FHHeader(title: 'System Configurations'),
              if (config.any((cfg) => cfg.key == 'site.url'))
                ExpansionTile(
                  title: SelectableText(
                    'Site Configuration',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  subtitle: Text('Configure your FeatureHub system',
                      style: Theme.of(context).textTheme.titleSmall),
                  controlAffinity: ListTileControlAffinity.leading,
                  children: [
                    Card(child: SiteSystemConfigWidget(knownConfigs: config))
                  ],
                ),
              ExpansionTile(
                title: SelectableText(
                  'Slack Configuration',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                subtitle: Text('Enable FeatureHub to send Slack messages',
                    style: Theme.of(context).textTheme.titleSmall),
                controlAffinity: ListTileControlAffinity.leading,
                children: [
                  if (!isSlackEnabled)
                    const Row(
                      children: [
                        Text(
                            "You are required to configure encryption key/password in the FeatureHub system properties file to enable Slack integration"),
                        SizedBox(
                          width: 16.0,
                        ),
                        FHExternalLinkWidget(
                          tooltipMessage: "View documentation",
                          link:
                              "https://docs.featurehub.io/featurehub/latest/configuration.html#_encryption",
                          icon: Icon(Icons.arrow_outward_outlined),
                          label: 'Encryption documentation',
                        )
                      ],
                    ),
                  if (isSlackEnabled)
                    Card(child: SlackSystemConfigWidget(knownConfigs: config))
                ],
              )
            ],
          );
        });
  }
}
