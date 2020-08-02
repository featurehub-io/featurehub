import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:flutter/material.dart';

class FHIconButton extends StatelessWidget {
  final Icon icon;
  final VoidCallback onPressed;
  final double width;
  final String tooltip;

  const FHIconButton({
    Key key,
    this.icon,
    this.tooltip,
    this.onPressed,
    this.width,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CustomCursor(
      child: Container(
        padding: const EdgeInsets.all(0.0),
        width: width ?? 48.0, // you can adjust the width as you need
        child: IconButton(
          splashRadius: 1.0,
          icon: icon,
          tooltip: tooltip,
          iconSize: 20,
          onPressed: onPressed,
        ),
      ),
    );
  }
}
