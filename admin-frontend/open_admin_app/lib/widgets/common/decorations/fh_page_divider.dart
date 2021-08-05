import 'package:flutter/material.dart';

class FHPageDivider extends StatelessWidget {
  const FHPageDivider({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        decoration: const BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.black87, width: 0.5))));
  }
}
