import 'package:flutter/material.dart';

class FHInfoCardWidget extends StatelessWidget {
  final String message;

  const FHInfoCardWidget({Key? key, required this.message}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      richMessage: WidgetSpan(
          alignment: PlaceholderAlignment.baseline,
          baseline: TextBaseline.alphabetic,
          child: Container(
            padding: const EdgeInsets.all(10),
            constraints: const BoxConstraints(maxWidth: 250),
            child: Text(message),
          )),
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColorLight,
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
