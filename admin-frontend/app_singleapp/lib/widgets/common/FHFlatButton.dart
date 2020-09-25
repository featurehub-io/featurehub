import 'package:flutter/material.dart';

class FHFlatButton extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButton(
      {Key key,
      @required this.onPressed,
      @required this.title,
      this.keepCase = false})
      : assert(title != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FlatButton(
      onPressed: onPressed,
      child: Text(keepCase ? title : title.toUpperCase(),
          style: Theme.of(context)
              .textTheme
              .subtitle2
              .merge(TextStyle(color: Colors.white))),
      color: Theme.of(context).buttonColor,
    );
  }
}
