import 'package:flutter/material.dart';

class FlagOnOffColoredIndicator extends StatelessWidget {
  final bool on;

  const FlagOnOffColoredIndicator({Key? key, this.on = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return on
        ? Text('ON',
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                    color: const Color(0xff11C8B5),
                    // fontWeight: FontWeight.bold
            ))
        : Text('OFF',
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                    color: const Color(0xffF44C49),
                    // fontWeight: FontWeight.bold
            ));
  }
}
