import 'package:flutter/material.dart';

class FHPageDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        decoration: const BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.black87, width: 0.5))));
  }
}
