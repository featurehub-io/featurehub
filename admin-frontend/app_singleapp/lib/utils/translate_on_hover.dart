import 'package:flutter/material.dart';

class TranslateOnHover extends StatefulWidget {
  final Widget child;
  TranslateOnHover({Key key, this.child}) : super(key: key);

  @override
  _TranslateOnHoverState createState() => _TranslateOnHoverState();
}

class _TranslateOnHoverState extends State<TranslateOnHover> {
  bool _hovering = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (e) => _mouseEnter(true),
      onExit: (e) => _mouseEnter(false),
      child: Container(
        child: widget.child,
        decoration: _hovering ? BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: Theme.of(context).buttonColor,
              width: 1.0,
            ),
          ),
        ) : BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: Colors.transparent,
              width: 1.0,
            ),
          ),
        ),
      ),
    );
  }

  void _mouseEnter(bool hover) {
    setState(() {
      _hovering = hover;
    });
  }
}


