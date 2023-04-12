import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class NotSetContainer extends StatelessWidget {
  const NotSetContainer({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: Container(
          margin: const EdgeInsets.only(top: 8.0),
          padding: const EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
          decoration: BoxDecoration(
            borderRadius: const BorderRadius.all(Radius.circular(16.0)),
            border: Border.all(
              color: Theme.of(context).disabledColor,
              width: 1,
            ),
          ),
          child: Text('not set',
              style: GoogleFonts.openSans(
                  textStyle: Theme.of(context).textTheme.bodyLarge))),
    );
  }
}
