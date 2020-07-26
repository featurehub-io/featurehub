import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHIconTextButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String label;
  final bool keepCase;
  final IconData iconData;

  const FHIconTextButton(
      {Key key,
      this.onPressed,
      @required this.label,
      this.keepCase = false,
      @required this.iconData})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FlatButton.icon(
        mouseCursor: SystemMouseCursors.click,
        icon: Icon(iconData, color: Theme.of(context).buttonColor),
        textColor: Theme.of(context).buttonColor,
        onPressed: onPressed,
        label: Text(
          keepCase ? label : label.toUpperCase(),
        ));
  }
}
