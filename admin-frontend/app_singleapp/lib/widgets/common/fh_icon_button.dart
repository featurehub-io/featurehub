import 'package:flutter/material.dart';

class FHIconButton extends StatelessWidget {
  final Icon icon;
  final VoidCallback onPressed;
  final String? tooltip;

  const FHIconButton({
    Key? key,
    required this.icon,
    this.tooltip,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(
      // wrapping in material so the hovering highlight works
      type: MaterialType
          .transparency, // need this for dark mode, so the icon has transparent background
      child: IconButton(
        splashRadius: 20.0,
        icon: icon,
        color: Theme.of(context).buttonColor,
        tooltip: tooltip,
        iconSize: 20,
        onPressed: onPressed,
      ),
    );
  }
}
