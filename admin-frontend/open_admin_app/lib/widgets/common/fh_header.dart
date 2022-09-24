import 'package:flutter/material.dart';

class FHHeader extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const FHHeader({
    Key? key,
    required this.title,
    this.children = const [],
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var titleChildren = <Widget>[];

    titleChildren.add(
      Container(
        padding: const EdgeInsets.only(right: 24),
        child: SelectableText(title, style: Theme.of(context).textTheme.headline5
            // .copyWith(color: Theme.of(context).primaryColor
            ),
      ),
    );

    titleChildren = List.from(titleChildren)..addAll(children);

    return FittedBox(
      child: Container(
        padding: const EdgeInsets.fromLTRB(10, 10, 25, 0),
        child: Row(
          children: titleChildren,
        ),
      ),
    );
  }
}
