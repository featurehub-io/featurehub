import 'package:flutter/material.dart';

class FHIconTextButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String label;
  final bool keepCase;
  final IconData iconData;
  final double size;
  final Color color;

  const FHIconTextButton({
    Key key,
    this.onPressed, @ required this.label, this.keepCase=false, @ required this.iconData, this.size, this.color
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FlatButton.icon(
        icon: Icon(iconData, color: color ?? Theme.of(context).buttonColor, size: size,),
        textColor: Theme.of(context).buttonColor,
        onPressed:onPressed,
        label: Text(keepCase ? label : label.toUpperCase(), style: TextStyle(color: color)
    ));
  }
}
