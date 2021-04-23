import 'package:flutter/material.dart';

class FHFlatButtonTransparent extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonTransparent(
      {Key? key, this.onPressed, this.title, this.keepCase = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(keepCase ? title : title.toUpperCase(),
              style: TextStyle(color: Theme.of(context).buttonColor))),
    );
  }
}
