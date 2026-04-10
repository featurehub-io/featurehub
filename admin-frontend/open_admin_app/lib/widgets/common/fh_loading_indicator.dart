import 'package:flutter/material.dart';

class FHLoadingIndicator extends StatelessWidget {
  const FHLoadingIndicator({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(child: Padding(
      padding: EdgeInsets.all(8.0),
      child: CircularProgressIndicator(),
    ));
  }
}
