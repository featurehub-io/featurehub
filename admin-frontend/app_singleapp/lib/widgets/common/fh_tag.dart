import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

enum TagStatus { active, inactive, disabled }

class FHTagWidget extends StatelessWidget {
  final TagStatus state;
  final String text;
  final TextStyle style;

  const FHTagWidget({Key key, @required this.text, this.state, this.style})
      : super(key: key);

  Widget build(BuildContext context) {
    Color labelColor = Theme.of(context).dividerColor;
    if (state == TagStatus.active) {
      labelColor = Theme.of(context).toggleableActiveColor;
    }
    if (state == TagStatus.inactive) {
      labelColor = Theme.of(context).disabledColor;
    }

    return Align(
      alignment: Alignment.bottomLeft,
      child: Container(
        width: 100,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(10.0)),
          color: labelColor != null ? labelColor : null,
        ),
        alignment: Alignment.center,
        padding: EdgeInsets.only(top: 5.0, left: 8, right: 8, bottom: 5.0),
        child: Text(
          text,
          style: style ??
              Theme.of(context).textTheme.bodyText2.copyWith(
                  color: Theme.of(context).colorScheme.onPrimary,
                  fontSize: 13,),
          overflow: TextOverflow.ellipsis,
        ),
      ),
    );
  }
}
