import 'package:flutter/material.dart';

class FHFlatButtonGreen extends StatelessWidget {
  final Function onPressed;
  final String title;


  const FHFlatButtonGreen(
      {Key? key,
        required this.onPressed,
        required this.title,
      })
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: () => onPressed.call(),
      style:
      TextButton.styleFrom(backgroundColor: Colors.green),
      child: Text(title),
    );
  }
}
