import 'package:flutter/material.dart';

class ConfigurationViewerField extends StatelessWidget {
  final String text;
  final bool canEdit;

  const ConfigurationViewerField({
    Key key,
    @required this.text,
    this.canEdit,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (text != null && text.isNotEmpty) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Flexible(
            flex: 4,
            child: Text(
              text.replaceAll('\n', ''),
              style: TextStyle(fontFamily: 'source', fontSize: 12),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Flexible(flex: 1, child: Icon(Icons.more_horiz))
        ],
      );
    } else if (canEdit) {
      return Text(
        'Edit value',
        style: Theme.of(context).textTheme.caption,
      );
    }
    return Text('No editing permissions',
        style: Theme.of(context).textTheme.caption);
  }
}
