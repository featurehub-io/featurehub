import 'package:flutter/material.dart';

class FHFlatButtonAccent extends StatelessWidget {
  final Function onPressed;
  final String title;
  final bool keepCase;

  const FHFlatButtonAccent(
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
          TextButton.styleFrom(backgroundColor: Theme.of(context).accentColor),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Text(
          keepCase ? title : title.toUpperCase(),
          style: TextStyle(
              color: Theme.of(context).brightness == Brightness.light
                  ? Colors.white
                  : Colors.black),
        ),
      ),
    );
  }
}
