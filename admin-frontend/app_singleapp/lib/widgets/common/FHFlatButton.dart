import 'package:app_singleapp/utils/custom_cursor.dart';
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
    return CustomCursor(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: FlatButton(
          onPressed: onPressed,
          child: Text(keepCase ? title : title.toUpperCase(),
              style: Theme.of(context)
                  .textTheme
                  .subtitle2
                  .merge(TextStyle(color: Colors.white))),
          color: Theme.of(context).buttonColor,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(5.0)),
        ),
      ),
    );
  }
}
