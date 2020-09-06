import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHUnderlineButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;
  final bool enabled;

  const FHUnderlineButton({
    Key key,
    this.onPressed, this.title, this.keepCase=true, this.enabled
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return InkWell(
        mouseCursor: enabled ? SystemMouseCursors.click : null,
        onTap: onPressed,
        child: Text(keepCase ? title : title.toUpperCase(),
            style: Theme.of(context).textTheme.bodyText1.copyWith(
                decoration: TextDecoration.underline,
                color: enabled ? Theme.of(context).buttonColor : Colors.black54)));
  }
}
