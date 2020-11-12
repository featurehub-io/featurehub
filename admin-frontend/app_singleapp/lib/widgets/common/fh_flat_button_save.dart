import 'package:flutter/material.dart';

class FHFlatButtonSave extends StatelessWidget {
  final VoidCallback onPressedFunc;


  const FHFlatButtonSave({Key key, this.onPressedFunc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FlatButton(
      onPressed: () {
        onPressedFunc();
      },
      child: Text('SAVE',
        style: Theme.of(context).textTheme.subtitle2.merge(TextStyle(color: Colors.white))
      ),
      color: Theme.of(context).buttonColor,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5.0)),
    );
  }
}
