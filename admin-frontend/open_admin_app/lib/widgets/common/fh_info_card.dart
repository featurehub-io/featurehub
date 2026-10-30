import 'package:flutter/material.dart';

class FHInfoCardWidget extends StatelessWidget {
  final String message;

  const FHInfoCardWidget({super.key, required this.message});

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      richMessage: WidgetSpan(
          alignment: PlaceholderAlignment.baseline,
          baseline: TextBaseline.alphabetic,
          child: Container(
            padding: const EdgeInsets.all(10),
            constraints: const BoxConstraints(maxWidth: 250),
            child: Text(
              message,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSecondaryContainer,
              ),
            ),
          )),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.secondaryContainer,
        borderRadius: const BorderRadius.all(Radius.circular(4)),
      ),
      waitDuration: const Duration(milliseconds: 300),
      verticalOffset: 10,
      child: const Icon(
        Icons.info_outline,
        size: 20,
      ),
    );
  }
}
