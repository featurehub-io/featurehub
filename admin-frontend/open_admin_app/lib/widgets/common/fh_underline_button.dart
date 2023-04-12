import 'package:flutter/material.dart';

class FHUnderlineButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;

  const FHUnderlineButton(
      {Key? key,
      required this.onPressed,
      required this.title,
      })
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(onPressed: onPressed, icon: Text(title), label: const Icon(Icons.arrow_forward_rounded));
  }
}
