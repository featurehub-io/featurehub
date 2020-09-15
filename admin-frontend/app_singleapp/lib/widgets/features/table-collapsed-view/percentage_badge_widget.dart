import 'package:flutter/material.dart';

class PercentageBadgeWidget extends StatelessWidget {
  const PercentageBadgeWidget({
    Key key,
    @required this.percentage,
  }) : super(key: key);

  final int percentage;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 8.0),
      child: Container(
        constraints: BoxConstraints(minWidth: 38.0),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(4.0)),
          color: Color(0xffe2f5fd),
        ),
        alignment: Alignment.center,
        height: 22,
        child: Text('${(percentage / 100).toString()}%',
            style: Theme.of(context).textTheme.overline),
      ),
    );
  }
}
