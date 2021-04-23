import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class PageNotFoundRoute extends StatelessWidget {
  PageNotFoundRoute({Key? key, this.title}) : super(key: key);
  final String title;

  @override
  Widget build(BuildContext context) {
    return FHHeader(
      title: "Looks like this page doesn't exist!",
//      Container(
//        child: Column(
//          crossAxisAlignment: CrossAxisAlignment.start,
//          children: [
//            Text("Looks like this page doesn't exist!",
//                style: Theme.of(context).textTheme.headline6)
//          ],
//        ),
//      ),
    );
  }
}
