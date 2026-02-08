import 'package:flutter/material.dart';

class CustomTextStyle {
  static TextStyle bodySmallLight(BuildContext context) {
    return Theme.of(context).textTheme.bodySmall!.copyWith(
        color: Theme.of(context).textTheme.bodySmall!.color!.withAlpha(153));
  }

  static TextStyle bodyMediumBold(BuildContext context) {
    return const TextStyle(fontWeight: FontWeight.bold);
  }
}
