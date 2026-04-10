import 'package:flutter/material.dart';

class FHFlatButton extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButton(
      {super.key,
      required this.onPressed,
      required this.title,
      this.keepCase = false});

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      onPressed: () => onPressed.call(),
      child: Text(title),
    );
  }
}
