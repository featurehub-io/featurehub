import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHCardWidget extends StatelessWidget {
  final Widget child;
  final double width;

  const FHCardWidget({Key key, @required this.child, this.width = 800})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(maxWidth: width),
      padding: const EdgeInsets.only(top: 10.0),
      child: Card(
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(3.0)),
          child: Padding(padding: const EdgeInsets.all(20.0), child: child)),
    );
  }
}
