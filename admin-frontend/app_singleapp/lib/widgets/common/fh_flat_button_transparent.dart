import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHFlatButtonTransparent extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonTransparent(
      {Key key, this.onPressed, this.title, this.keepCase = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FlatButton(
        mouseCursor: SystemMouseCursors.click,
        onPressed: onPressed,
        child: Text(keepCase ? title : title.toUpperCase(),
            style: Theme.of(context)
                .textTheme
                .subtitle2
                .merge(TextStyle(color: Theme.of(context).buttonColor))));
  }
}
