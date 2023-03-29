import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';

class AdminAccessKeyDisplayWidget extends StatelessWidget {
  const AdminAccessKeyDisplayWidget({Key? key, required this.token})
      : super(key: key);

  final String token;

  @override
  Widget build(BuildContext context) {
    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(top: 20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text('Access token',
                    style: Theme.of(context).textTheme.titleSmall),
                Padding(
                  padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                  child: SelectableText(
                    token,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
          ),
          FHCopyToClipboardFlatButton(
            text: token,
            caption: ' Copy access token to clipboard',
          ),
          const SizedBox(height: 8.0),
          Text(
            'For security, you will not be able to view the access token once you close this window.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ]);
  }
}
