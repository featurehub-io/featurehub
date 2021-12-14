import 'package:flutter/material.dart';

enum TagStatus { active, inactive, disabled }

class FHTagWidget extends StatelessWidget {
  final TagStatus state;
  final String text;
  final TextStyle? style;

  const FHTagWidget(
      {Key? key, required this.text, required this.state, this.style})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var labelColor = Theme.of(context).dividerColor;
    if (state == TagStatus.active) {
      labelColor = Theme.of(context).toggleableActiveColor;
    }
    if (state == TagStatus.inactive) {
      labelColor = Theme.of(context).disabledColor;
    }

    return Align(
      alignment: Alignment.topCenter,
      child: Container(
        width: 100,
        height: 30,
        margin: const EdgeInsets.all(8.0),
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.all(Radius.circular(10.0)),
          color: labelColor,
        ),
        alignment: Alignment.center,
        padding:
            const EdgeInsets.only(top: 5.0, left: 8, right: 8, bottom: 5.0),
        child: Text(
          text,
          style: style ??
              Theme.of(context).textTheme.bodyText2!.copyWith(
                    color: Theme.of(context).colorScheme.onPrimary,
                    fontSize: 13,
                  ),
          overflow: TextOverflow.ellipsis,
        ),
      ),
    );
  }
}
