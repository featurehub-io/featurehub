import 'package:flutter/material.dart';

class FhAttentionWidget extends StatelessWidget {
  final String text;

  const FhAttentionWidget({super.key, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Icon(Icons.lightbulb_outline_sharp,
            size: 24.0, color: Colors.orange),
        const SizedBox(width: 4.0),
        Text(text,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: Colors.orange)),
      ],
    );
  }
}
