import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/features/features_latest_widget.dart';
import 'package:flutter/material.dart';

class FeaturesLatestRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return  _FeaturesLatestWidget();
  }
}

class _FeaturesLatestWidget extends StatefulWidget{
  @override
  State<StatefulWidget> createState() => _FeaturesLatestState();
}

class _FeaturesLatestState extends State<_FeaturesLatestWidget>{
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(),
        LatestFeaturesWidget()
      ],
    );
  }

  Widget _headerRow() {
    return Container(
      padding: const EdgeInsets.fromLTRB(0, 0, 30, 10),
      child: FHHeader(
        title: "Recent updates",
      ));
  }
}

