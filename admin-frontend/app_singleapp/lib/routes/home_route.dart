import 'package:flutter/material.dart';

class HomeRoute extends StatelessWidget {
  HomeRoute({Key key, this.title}) : super(key: key);
  final String title;

  @override
  Widget build(BuildContext context) {
    return Center(child: Text('My Page!'));
  }
}
