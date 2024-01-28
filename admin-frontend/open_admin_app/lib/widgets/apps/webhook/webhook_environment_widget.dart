import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/webhook/slack_panel_widget.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_environment_table_widget.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';

class WebhookEnvironment extends StatefulWidget {
  const WebhookEnvironment({Key? key}) : super(key: key);

  @override
  State<WebhookEnvironment> createState() => _WebhookEnvironmentState();
}

class _WebhookEnvironmentState extends State<WebhookEnvironment> {
  late WebhookEnvironmentBloc bloc;
  WebhookTypeDetail? _currentWebhookType;
  String? _envId;

  @override
  void initState() {
    super.initState();

    bloc = BlocProvider.of<WebhookEnvironmentBloc>(context);
    _currentWebhookType = bloc.currentWebhookType;
    _envId = bloc.current?.id;
  }

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 16.0),
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Environment',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (bloc.environments.isNotEmpty == true)
                  selectEnvironment(bloc.environments),
                if (bloc.environments.isNotEmpty != true)
                  const Text("no environments")
              ],
            ),
            const SizedBox(
              width: 16.0,
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Integration Type',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                selectWebhookTypeDetail(),
              ],
            ),
            const SizedBox(
              width: 32,
            ),
            const FHExternalLinkWidget(
              tooltipMessage: "View documentation",
              link:
                  "https://docs.featurehub.io/featurehub/latest/webhooks.html",
              icon: Icon(Icons.arrow_outward_outlined),
              label: 'Integrations Documentation',
            ),
          ]),
          StreamBuilder<EnvironmentAndWebhookType>(
            stream: bloc.environmentStream,
            builder: (context, snapshot) {
              if (snapshot.data == null || _currentWebhookType == null) {
                return const SizedBox.shrink();
              }

              if (_currentWebhookType?.messageType ==
                  "webhook-environment-result-v1") {
                return WebhookEnvironmentTable(snapshot.data!, bloc);
              }

              if (_currentWebhookType?.messageType == "integration/slack-v1") {
                return SlackPanelWidget(snapshot.data!, bloc);
              }

              return const SizedBox.shrink();
            },
          ),
        ],
      ),
    ]);
  }

  Widget selectWebhookTypeDetail() {
    return Container(
      constraints: const BoxConstraints(maxWidth: 300),
      child: InkWell(
        mouseCursor: SystemMouseCursors.click,
        child: StreamBuilder<List<WebhookTypeDetail>?>(
          stream: bloc.mrBloc.streamValley.webhookTypeStream,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const FHLoadingIndicator();
            } else if (snapshot.connectionState == ConnectionState.active ||
                snapshot.connectionState == ConnectionState.done) {
              if (snapshot.hasError) {
                return const FHLoadingError();
              } else if (snapshot.hasData) {
                return webhookDetail(snapshot.data!);
              }
            }

            return const SizedBox.shrink();
          },
        ),
      ),
    );
  }

  Widget webhookDetail(List<WebhookTypeDetail> details) {
    return DropdownButton<WebhookTypeDetail>(
      hint: const Text(
        'Select webhook type',
        textAlign: TextAlign.end,
      ),
      icon: const Padding(
        padding: EdgeInsets.only(left: 8.0),
        child: Icon(
          Icons.keyboard_arrow_down,
          size: 18,
        ),
      ),
      isDense: true,
      isExpanded: true,
      items: details.map((WebhookTypeDetail whType) {
        return DropdownMenuItem<WebhookTypeDetail>(
            value: whType,
            child: Text(
              whType.description,
              style: Theme.of(context).textTheme.bodyMedium,
              overflow: TextOverflow.ellipsis,
            ));
      }).toList(),
      onChanged: (WebhookTypeDetail? value) {
        setState(() {
          _currentWebhookType = value;
          bloc.webhookType = value;
        });
      },
      value: _currentWebhookType,
    );
  }

  Widget selectEnvironment(List<Environment> environments) {
    return Container(
      constraints: const BoxConstraints(maxWidth: 300),
      child: InkWell(
        mouseCursor: SystemMouseCursors.click,
        child: DropdownButton<String>(
          hint: const Text(
            'Select environment',
            textAlign: TextAlign.end,
          ),
          icon: const Padding(
            padding: EdgeInsets.only(left: 8.0),
            child: Icon(
              Icons.keyboard_arrow_down,
              size: 18,
            ),
          ),
          isDense: true,
          isExpanded: true,
          items: bloc.environments.map((Environment environment) {
            return DropdownMenuItem<String>(
                value: environment.id,
                child: Text(
                  environment.name,
                  style: Theme.of(context).textTheme.bodyMedium,
                  overflow: TextOverflow.ellipsis,
                ));
          }).toList(),
          onChanged: (String? value) {
            setState(() {
              bloc.current =
                  bloc.environments.firstWhereOrNull((e) => e.id == value);
              _envId = value;
            });
          },
          value: _envId,
        ),
      ),
    );
  }
}
