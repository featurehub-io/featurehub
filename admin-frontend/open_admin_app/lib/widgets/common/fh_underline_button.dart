import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/translate_on_hover.dart';

class FHUnderlineButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String title;
  final bool keepCase;
  final bool enabled;
  final Color? color;

  const FHUnderlineButton(
      {Key? key,
      required this.onPressed,
      required this.title,
      this.keepCase = true,
      this.enabled = true,
      this.color})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      onTap: onPressed,
      child: Container(
        padding: const EdgeInsets.all(2.0),
        alignment: Alignment.centerLeft,
        child: TranslateOnHover(
          child: Text(
            title,
            style: Theme.of(context)
                .textTheme
                .button
                ?.copyWith(color: Colors.blue),
          ),
        ),
      ),
    );
  }
}
