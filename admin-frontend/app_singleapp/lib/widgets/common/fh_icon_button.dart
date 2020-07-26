import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHIconButton extends StatelessWidget {
  final Icon icon;
  final VoidCallback onPressed;
  final double width;

  const FHIconButton({
    Key key,
    this.icon,
    this.onPressed,
    this.width,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(0.0),
      width: width ?? 48.0, // you can adjust the width as you need
      child: IconButton(
        mouseCursor: SystemMouseCursors.click,
        splashRadius: 1.0,
        icon: icon,
        iconSize: 20,
        onPressed: onPressed,
      ),
    );
  }
}
