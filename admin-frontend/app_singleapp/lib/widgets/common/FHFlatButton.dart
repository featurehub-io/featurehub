import 'package:flutter/material.dart';

class FHFlatButton extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButton(
      {Key? key,
      required this.onPressed,
      required this.title,
      this.keepCase = false})
      : assert(title != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: () => onPressed.call(),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Text(title, style: TextStyle(color: Colors.white)),
      ),
      style:
          TextButton.styleFrom(backgroundColor: Theme.of(context).buttonColor),
    );
  }
}
