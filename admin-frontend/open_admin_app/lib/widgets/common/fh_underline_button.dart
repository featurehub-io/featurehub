import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/translate_on_hover.dart';

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
    return TextButton.icon(onPressed: onPressed, icon: Text(title), label: Icon(Icons.arrow_forward_rounded));
  }
}
