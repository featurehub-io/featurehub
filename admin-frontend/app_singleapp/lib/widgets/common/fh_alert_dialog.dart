import 'package:flutter/material.dart';

class FHAlertDialog extends StatelessWidget {
  final Widget title;
  final Widget content;
  final List<Widget> actions;

  const FHAlertDialog({Key? key, this.title, this.content, this.actions})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      ModalBarrier(dismissible: true, color: Colors.black54),
      AlertDialog(
        title: title,
        content: content,
        actions: actions,
        buttonPadding: EdgeInsets.symmetric(horizontal: 16.0),
      )
    ]);
  }
}
