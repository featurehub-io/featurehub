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
    return IconButton(
      icon: icon,
      color: Theme.of(context).colorScheme.primary,
      tooltip: tooltip,
      iconSize: 20,
      onPressed: onPressed,
    );
  }
}
