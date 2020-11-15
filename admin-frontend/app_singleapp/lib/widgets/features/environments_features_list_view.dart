import 'package:app_singleapp/widgets/features/feature_value_cell.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'feature_dashboard_constants.dart';
import 'tabs_bloc.dart';

class EnvironmentsAndFeatureValuesListView extends StatelessWidget {
  const EnvironmentsAndFeatureValuesListView({
    Key key,
    @required this.bloc,
  }) : super(key: key);

  final FeaturesOnThisTabTrackerBloc bloc;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<TabsState>(
        stream: bloc.currentTab,
        builder: (context, currentTabSnapshot) {
          return StreamBuilder<Set<String>>(
              stream: bloc.featureCurrentlyEditingStream,
              builder: (context, snapshot) {
                final unselHeight = bloc.unselectedFeatureCountForHeight;
                final selHeight = bloc.selectedFeatureCountForHeight;
                return Container(
                  height: unselHeight + selHeight + headerHeight + 2 + 35,
                  child: Scrollbar(
                    child: ListView(
                      scrollDirection: Axis.horizontal,
                      physics: ClampingScrollPhysics(),
                      children: [
                        if (bloc.features.isNotEmpty)
                          ...bloc.sortedEnvironmentsThatAreShowing.map((efv) {
                            return Container(
//                                  padding:
//                                      EdgeInsets.only(left: 1.0, right: 1.0),
                              width: cellWidth,
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.start,
                                children: [
                                  Container(
                                    width: cellWidth,
                                    color: Theme.of(context).highlightColor,
                                    height: headerHeight,
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        Text(efv.environmentName.toUpperCase(),
                                            overflow: TextOverflow.ellipsis,
                                            style: Theme.of(context)
                                                .textTheme
                                                .overline
                                                .copyWith(
                                                    color: Colors.black87,
                                                    fontSize: 14)),
                                      ],
                                    ),
                                  ),
                                  ...bloc.features.map((f) {
                                    return Container(
                                      decoration: BoxDecoration(
                                          border: Border(
                                        bottom: BorderSide(
                                            color: Colors.black45, width: 0.5),
                                        right: BorderSide(
                                            color: Colors.black45, width: 0.5),
                                      )),
                                      child: FeatureValueCell(
                                          tabsBloc: bloc,
                                          feature: f,
                                          value: efv.features.firstWhere(
                                              (fv) => fv.key == f.key,
                                              orElse: () => null),
                                          efv: efv),
                                    );
                                  }).toList(),
                                ],
                              ),
                            );
                          })
                      ],
                    ),
                  ),
                );
              });
        });
  }
}
