import 'package:flutter/material.dart';

class FHPageDivider extends StatelessWidget {
  const FHPageDivider({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        decoration: BoxDecoration(
            border: Border(
                bottom: BorderSide(
                    color: Theme.of(context).dividerColor, width: 0.5))));
  }
}
