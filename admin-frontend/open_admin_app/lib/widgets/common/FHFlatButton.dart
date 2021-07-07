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
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: () => onPressed.call(),
      style:
          TextButton.styleFrom(backgroundColor: Theme.of(context).buttonColor),
      child: Padding(
        padding: const EdgeInsets.only(top: 8.0, bottom: 8.0, left: 12.0, right: 16.0),
        child: Text(title, style: TextStyle(color: Colors.white)),
      ),
    );
  }
}
