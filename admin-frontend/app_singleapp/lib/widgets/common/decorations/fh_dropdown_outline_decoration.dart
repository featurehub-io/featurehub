import 'package:flutter/material.dart';

class DropDownOutlineDecoration extends StatelessWidget {
  final Widget child;

  const DropDownOutlineDecoration({Key key, @required this.child})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return OutlineButton(
        onPressed: () => {},
        shape: BeveledRectangleBorder(borderRadius: BorderRadius.circular(2)),
        padding: EdgeInsets.only(top: 4, bottom: 4, left: 12, right: 12),
        highlightColor: Theme.of(context).primaryColorLight,
        splashColor: Theme.of(context).primaryColorLight,
        borderSide: BorderSide(color: Colors.black, width: 2),
        child: child);
  }
}
