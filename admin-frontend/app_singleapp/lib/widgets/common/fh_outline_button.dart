import 'package:flutter/material.dart';

class FHOutlineButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;

  const FHOutlineButton({
    Key key,
    this.onPressed, this.title, this.keepCase=false
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
        // hoverColor: Theme.of(context).primaryColorLight,
        // borderSide: BorderSide(width: 2, color: Theme.of(context).buttonColor),
        onPressed:onPressed,
        child: Text(keepCase ? title : title.toUpperCase(),
            style: Theme.of(context)
                .textTheme
                .subtitle2
                .merge(TextStyle(color: Theme.of(context).buttonColor))));
  }
}
