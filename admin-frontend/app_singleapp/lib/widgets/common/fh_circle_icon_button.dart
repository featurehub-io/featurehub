import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class CircleIconButton extends StatelessWidget {
  final VoidCallback onTap;
  final Icon icon;

  const CircleIconButton({Key? key, required this.onTap, required this.icon})
      : super(key: key);
  @override
  Widget build(BuildContext context) {
    return ClipOval(
      child: Material(
        color: Theme.of(context).primaryColorLight, // button color
        child: InkWell(
          mouseCursor: SystemMouseCursors.click,
          splashColor: Theme.of(context).primaryColorDark, // inkwell color
          child: SizedBox(width: 32, height: 32, child: icon),
          onTap: onTap,
        ),
      ),
    );
  }
}
