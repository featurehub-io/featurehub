import 'package:flutter/material.dart';

class CustomTextStyle {
  static TextStyle bodySmallLight(BuildContext context) {
    return Theme.of(context).textTheme.bodySmall!.copyWith(
        color: Theme.of(context).textTheme.bodySmall!.color!.withOpacity(0.6));
  }

  static TextStyle bodyMediumBold(BuildContext context) {
    return TextStyle(fontWeight: FontWeight.bold);
  }
}
