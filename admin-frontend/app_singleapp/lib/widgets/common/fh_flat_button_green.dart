import 'package:flutter/material.dart';

class FHFlatButtonGreen extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonGreen(
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
      TextButton.styleFrom(backgroundColor: Colors.green),
      child: Padding(
        padding: const EdgeInsets.only(top: 8.0, bottom: 8.0, left: 12.0, right: 16.0),
        child: Text(title, style: TextStyle(color: Colors.white)),
      ),
    );
  }
}
