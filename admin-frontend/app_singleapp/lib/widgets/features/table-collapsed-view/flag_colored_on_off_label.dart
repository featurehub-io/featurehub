import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:app_singleapp/widgets/features/table-collapsed-view/value_not_set_container.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mrapi/api.dart';

class FlagOnOffColoredIndicator extends StatelessWidget {
  final bool on;

  const FlagOnOffColoredIndicator({Key key, this.on}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return on
        ? Text('ON',
            style: GoogleFonts.openSans(
                textStyle: Theme.of(context).textTheme.button.copyWith(
                    color: Color(0xff11C8B5), fontWeight: FontWeight.bold)))
        : Text('OFF',
            style: GoogleFonts.openSans(
                textStyle: Theme.of(context).textTheme.button.copyWith(
                    color: Color(0xffF44C49), fontWeight: FontWeight.bold)));
  }
}
