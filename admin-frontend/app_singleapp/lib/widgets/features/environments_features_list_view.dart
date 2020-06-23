import 'package:app_singleapp/widgets/features/feature_value_cell.dart';
import 'package:flutter/material.dart';

import 'feature_dashboard_constants.dart';
import 'hidden_environment_list.dart';
import 'tabs_bloc.dart';

class EnvironmentsAndFeatureValuesListView extends StatelessWidget {
  const EnvironmentsAndFeatureValuesListView({
    Key key,
    @required this.bloc,
  }) : super(key: key);

  final TabsBloc bloc;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: bloc.hiddenEnvironments,
        builder: (context, snapshot) {
          return StreamBuilder<TabsState>(
              stream: bloc.currentTab,
              builder: (context, snapshot) {
                return StreamBuilder<Set<String>>(
                    stream: bloc.featureCurrentlyEditingStream,
                    builder: (context, snapshot) {
                      return Container(
                        height: ((bloc.features.length -
                                    (snapshot.data?.length ?? 0)) *
                                unselectedRowHeight) +
                            ((snapshot.data?.length ?? 0) * selectedRowHeight) +
                            headerHeight,
                        child: ListView(
                          scrollDirection: Axis.horizontal,
                          children: [
                            if (bloc.features.isNotEmpty)
                              ...bloc.sortedEnvironmentsThatAreShowing
                                  .map((efv) {
                                return Container(
//                                  padding:
//                                      EdgeInsets.only(left: 1.0, right: 1.0),
                                  width: snapshot.data == TabsState.FLAGS
                                      ? 100.0
                                      : 170.0,
                                  child: Column(
                                    children: [
                                      Container(
//                                        color: Theme.of(context).highlightColor,
                                        height: headerHeight,
                                        child: Column(
                                          children: [
                                            HideEnvironmentContainer(
                                              name: efv.environmentName,
                                              envId: efv.environmentId,
                                            ),
                                          ],
                                        ),
                                      ),
                                      ...bloc.features.map((f) {
                                        return Container(
//                                          margin: EdgeInsets.symmetric(vertical: 1.0),
                                          decoration: BoxDecoration(
                                              border: Border(top: BorderSide())
//                                            borderRadius: BorderRadius.all(Radius.circular(4.0)),
//                                            color: Colors.black12,
                                              ),
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
                      );
                    });
              });
        });
  }
}
