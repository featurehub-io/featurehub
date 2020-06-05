import 'package:flutter/material.dart';

class CircleIconButton extends StatelessWidget {
  final VoidCallback onTap;
  final Icon icon;

  const CircleIconButton({Key key, this.onTap, this.icon}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return ClipOval(
      child: Material(
        color: Theme.of(context).primaryColorLight, // button color
        child: InkWell(
          splashColor: Theme.of(context).primaryColorDark, // inkwell color
          child: SizedBox(width: 32, height: 32, child: icon),
          onTap: onTap,
        ),
      ),
    );
  }
}
