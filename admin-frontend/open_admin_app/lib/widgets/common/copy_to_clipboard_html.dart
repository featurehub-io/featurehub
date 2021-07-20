import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';

typedef CopyToClipboardTextProvider = Future<String?> Function();

class FHCopyToClipboardFlatButton extends StatelessWidget {
  final String? text;
  final String? caption;
  final Text? captionText;
  final String? tooltip;
  final CopyToClipboardTextProvider? textProvider;

  const FHCopyToClipboardFlatButton(
      {Key? key,
      this.text,
      this.textProvider,
      this.caption,
      this.captionText,
      this.tooltip})
      : assert(caption != null || captionText != null),
        assert(text != null || textProvider != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    Widget fb = TextButton(
      onPressed: () async {
        final clipboardText = text ?? (await textProvider!());
        if (clipboardText != null) {
          await Clipboard.setData(ClipboardData(text: clipboardText));
        }
      },
      child: Row(
        children: <Widget>[
          const Padding(
            padding: EdgeInsets.only(right: 8.0),
            child: Icon(
              Icons.content_copy,
              size: 15.0,
            ),
          ),
          captionText ??
              Text(caption!,
                  style: Theme.of(context)
                      .textTheme
                      .subtitle2!
                      .merge(TextStyle(color: Theme.of(context).buttonColor))),
        ],
      ),
    );

    if (tooltip != null) {
      fb = Tooltip(
        message: tooltip!,
        child: fb,
      );
    }

    return fb;
  }
}

class FHCopyToClipboard extends StatelessWidget {
  const FHCopyToClipboard({
    Key? key,
    required this.tooltipMessage,
    required this.copyString,
  }) : super(key: key);

  final String tooltipMessage;
  final String copyString;

  @override
  Widget build(BuildContext context) {
    return FHIconButton(
      icon: const Icon(Icons.content_copy, size: 16.0),
      tooltip: tooltipMessage,
      onPressed: () async {
        await Clipboard.setData(ClipboardData(text: copyString));
      },
    );
  }
}
