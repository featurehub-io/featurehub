import 'package:flutter/material.dart';

class TranslateOnHover extends StatefulWidget {
  final Widget child;
  const TranslateOnHover({Key? key, required this.child}) : super(key: key);

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
        decoration: _hovering
            ? const BoxDecoration(
                border: Border(
                  bottom: BorderSide(
                    color: Colors.blue,
                    width: 1.0,
                  ),
                ),
              )
            : const BoxDecoration(
                border: Border(
                  bottom: BorderSide(
                    color: Colors.transparent,
                    width: 1.0,
                  ),
                ),
              ),
        child: widget.child,
      ),
    );
  }

  void _mouseEnter(bool hover) {
    setState(() {
      _hovering = hover;
    });
  }
}
