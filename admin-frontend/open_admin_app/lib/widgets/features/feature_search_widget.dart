

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/features/features_overview_table_widget.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class FeatureSearchWidget extends StatefulWidget {
  final TabSelectedBloc tabSelectedBloc;

  const FeatureSearchWidget({Key? key, required this.tabSelectedBloc}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _FeatureSearchState();
  }
}

class _FeatureSearchState extends State<FeatureSearchWidget> {
  final TextEditingController _search = TextEditingController();
  final Map<FeatureGrouping, String> filters = {};

  FeatureGrouping? currentGrouping;
  StreamSubscription<FeatureGrouping?>? currentGroupingListener;

  @override
  void initState() {
    super.initState();

    currentGroupingListener = widget.tabSelectedBloc.currentGrouping.listen((grouping) {
      currentGrouping = grouping;

      setState(() {
        _search.text = filters.putIfAbsent(grouping, () => '');
      });
    });

    // TODO:
    // if we want to empty out the search when swapping portfolios, we need to listen
    // to this and clear out the search when the application id changes
    // widget.bloc.allUpdateRequests.listen((featureUpdate) {
    //
    // });
  }

  @override
  void dispose() {
    super.dispose();

    currentGroupingListener?.cancel();
    currentGroupingListener = null;
  }

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      Expanded(child: TextField(controller: _search, key: ValueKey('feature-search'))),
      FHIconButton(icon: const Icon(Icons.search), onPressed: () {
        if (currentGrouping != null) {
          widget.tabSelectedBloc.featureStatusBloc.updateFeatureGrouping(currentGrouping!, _search.text, 0);
          filters[currentGrouping!] = _search.text;
        }
      })
    ],);
  }

}
