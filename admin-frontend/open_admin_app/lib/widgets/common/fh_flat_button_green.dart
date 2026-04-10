import 'package:flutter/material.dart';

class FHFlatButtonGreen extends StatelessWidget {
  final Function onPressed;
  final String title;


  const FHFlatButtonGreen(
      {super.key,
        required this.onPressed,
        required this.title,
      });

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
