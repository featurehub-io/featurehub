import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
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

    final l10n = AppLocalizations.of(context)!;

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
              FHHeader(title: l10n.systemConfigurationsTitle),
              if (config.any((cfg) => cfg.key == 'site.url'))
                ExpansionTile(
                  title: SelectableText(
                    l10n.siteConfigurationTitle,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  subtitle: Text(l10n.siteConfigurationSubtitle,
                      style: Theme.of(context).textTheme.titleSmall),
                  controlAffinity: ListTileControlAffinity.leading,
                  children: [
                    Card(child: SiteSystemConfigWidget(knownConfigs: config))
                  ],
                ),
              ExpansionTile(
                title: SelectableText(
                  l10n.slackConfigurationTitle,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                subtitle: Text(l10n.slackConfigurationSubtitle,
                    style: Theme.of(context).textTheme.titleSmall),
                controlAffinity: ListTileControlAffinity.leading,
                children: [
                  if (!isSlackEnabled)
                    Row(
                      children: [
                        Text(l10n.encryptionRequiredForSlack),
                        const SizedBox(
                          width: 16.0,
                        ),
                        FHExternalLinkWidget(
                          tooltipMessage: l10n.viewDocumentation,
                          link:
                              "https://docs.featurehub.io/featurehub/latest/configuration.html#_encryption",
                          icon: const Icon(Icons.arrow_outward_outlined),
                          label: l10n.encryptionDocumentation,
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
