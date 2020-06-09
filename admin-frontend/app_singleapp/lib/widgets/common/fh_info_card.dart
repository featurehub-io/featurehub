import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHInfoCardWidget extends StatelessWidget {
  final String message;

  const FHInfoCardWidget({Key key, @required this.message}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      textStyle: Theme.of(context)
          .textTheme
          .bodyText2
          .copyWith(color: Theme.of(context).primaryColor),
      child: InkWell(
        radius: 36.0,
        onHover: (_) {},
        onTap: () {},
        child: Icon(
          Icons.info,
          size: 22.0,
        ),
      ),
      message: message,
      padding: EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColorLight.withOpacity(0.9),
        borderRadius: const BorderRadius.all(Radius.circular(4)),
      ),
      verticalOffset: 20,
      waitDuration: Duration(milliseconds: 600),
    );
  }
}
