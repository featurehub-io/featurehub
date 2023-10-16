import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';

class WebhookDetailTable extends StatelessWidget {
  final WebhookEnvironmentBloc bloc;
  final WebhookDetail data;

  const WebhookDetailTable(this.data, this.bloc, {Key? key})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 800),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              for(var count = 0; count < 9; count ++)
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    children: [
                      if (count != 8)
                        SizedBox(
                          width: 120,
                          child: Text(byRow(count), style: Theme.of(context).textTheme.bodySmall),
                        ),
                      Expanded(
                          child: Text(byRowContent(count), softWrap: false,
                            overflow: TextOverflow.ellipsis, maxLines: 100,)),
                      if (count == 8)
                        FHCopyToClipboard(
                            copyString: byRowContent(count),
                            tooltipMessage: 'Copy Content')
                    ],
                  ),
                )
            ],
          ),
        ),
      ),
    );
  }

  String byRow(int index) {
    switch (index) {
      case 0:
        return 'When sent';
      case 1:
        return 'Webhook Cloud Event type';
      case 2:
        return 'URL';
      case 3:
        return 'Method';
      case 4:
        return 'HTTP status';
      case 5:
        return 'Cloud Event type';
      case 6:
        return 'Incoming headers';
      case 7:
        return 'Outgoing headers';
    }

    return 'Webhook Content';
  }

  String byRowContent(int index) {
    switch (index) {
      case 0:
        return data.whenSent.toString() ?? '';
      case 1:
        return data.deliveredDataCloudEventType ?? '';
      case 2:
        return data.url ?? '';
      case 3:
        return data.method;
      case 4:
        return data.status.toString();
      case 5:
        return data.cloudEventType;
      case 6:
        return keyValue(data.incomingHeaders ?? {});
      case 7:
        return keyValue(data.outboundHeaders ?? {});
    }

    return data.content ?? '';
  }

  String keyValue(Map<String, String> vals) =>
      vals.entries.map((val) => "${val.key}: ${val.value}").join("\n");
}
