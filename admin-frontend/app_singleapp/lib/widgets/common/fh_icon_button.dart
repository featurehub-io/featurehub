import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:flutter/material.dart';

class FHIconButton extends StatelessWidget {

  final Icon icon;
  final VoidCallback onPressed;
  const FHIconButton({
    Key key, this.icon, this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CustomCursor(
      child: IconButton(
        icon: icon,
        iconSize: 20,
        onPressed: onPressed,
      ),
    );
  }
}
