import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class NotSetContainer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: Container(
        margin: EdgeInsets.only(top: 8.0),
          padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.all(Radius.circular(16.0)),
            border: Border.all(
              color: Theme.of(context).disabledColor,
              width: 1,
            ),
          ),
          child: Text('not set',
              style: GoogleFonts.openSans(
                  textStyle: Theme.of(context).textTheme.bodyText1))),
    );
  }
}
