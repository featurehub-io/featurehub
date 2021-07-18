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
    return ElevatedButton(
      onPressed: () => onPressed.call(),
      child: Text(title),
    );
  }
}
