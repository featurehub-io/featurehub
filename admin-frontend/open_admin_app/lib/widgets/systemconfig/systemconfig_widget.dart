

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/systemconfig/slack/slack_system_config_widget.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';

class SystemConfigPanel extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final sysBloc = BlocProvider.of<SystemConfigBloc>(context);

    FHAnalytics.sendScreenView("system-config-panel");

    return StreamBuilder<List<SystemConfig>?>(
      stream: sysBloc.knownConfigStream,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return FHLoadingError();
        }
        if (!snapshot.hasData) {
          return FHLoadingIndicator();
        }

        final config = snapshot.data!;

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FHHeader(title: 'System Configurations'),
            if (config.any((cfg) => cfg.key == 'slack.enabled'))
              ExpansionTile(
                title: SelectableText(
                  'Slack Configuration',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                subtitle: Text('Enable FeatureHub to send Slack messages', style: Theme.of(context).textTheme.titleSmall),
                controlAffinity: ListTileControlAffinity.leading,
                children: [
                  Card(child: SlackSystemConfigWidget(knownConfigs: config))
                ],
              )
          ],
        );
      }
    );
  }
}
