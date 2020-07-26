import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:google_fonts/google_fonts.dart';

class FHFlatButtonAccent extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonAccent(
      {Key key,
      @required this.onPressed,
      @required this.title,
      this.keepCase = false})
      : assert(title != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: FlatButton(
        mouseCursor: SystemMouseCursors.click,
        onPressed: onPressed,
        child: Text(
          keepCase ? title : title.toUpperCase(),
          style: GoogleFonts.openSans(
              textStyle: Theme.of(context)
                  .textTheme
                  .subtitle2
                  .merge(TextStyle(color: Colors.white))),
        ),
        color: Colors.orange,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5.0)),
      ),
    );
  }
}
