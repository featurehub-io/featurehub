import 'package:flutter/material.dart';

class FHButtonBar extends StatelessWidget {
  final List<Widget> children;

  const FHButtonBar({
    Key? key,
    required this.children,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(0, 30, 20, 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: children,
      ),
    );
  }
}
