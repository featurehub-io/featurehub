import 'package:flutter/material.dart';

class FHButtonBar extends StatelessWidget {
  final List<Widget> children;

  const FHButtonBar({
    super.key,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(0, 30, 20, 10),
      child: OverflowBar(
        alignment: MainAxisAlignment.end,
        children: children,
      ),
    );
  }
}
