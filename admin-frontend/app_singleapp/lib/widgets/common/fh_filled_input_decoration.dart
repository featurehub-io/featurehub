import 'package:flutter/material.dart';

// putting an inputDecoration on a dropdown forces us to set a width,
// this can create more problems than it solves,
// only use this where you need to fix the width
InputDecoration FHFilledInputDecoration({String labelText, bool filled = true}) {
  return InputDecoration(
      contentPadding: EdgeInsets.symmetric(vertical: 12.0, horizontal: 10.0),
      filled: filled,
      labelText: labelText);
}
