import 'package:flutter/material.dart';

class FHLoadingIndicator extends StatelessWidget {
  const FHLoadingIndicator({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const Center(child: Padding(
      padding: EdgeInsets.all(8.0),
      child: CircularProgressIndicator(),
    ));
  }
}
