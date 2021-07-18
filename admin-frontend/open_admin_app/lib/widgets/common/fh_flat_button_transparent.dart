import 'package:flutter/material.dart';

class FHFlatButtonTransparent extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonTransparent(
      {Key? key,
      required this.onPressed,
      required this.title,
      this.keepCase = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      child: Text(keepCase ? title : title.toUpperCase(),
    ),
    );
  }
}
