import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHUnderlineButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;

  const FHUnderlineButton({
    Key key,
    this.onPressed, this.title, this.keepCase=true
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CustomCursor(
      child: InkWell(
          mouseCursor: SystemMouseCursors.click,
          onTap: onPressed,
          child: Text(keepCase ? title : title.toUpperCase(),
              style: Theme.of(context).textTheme.bodyText1.copyWith(
                  decoration: TextDecoration.underline,
                  color: Theme.of(context).buttonColor))),
    );
  }
}
