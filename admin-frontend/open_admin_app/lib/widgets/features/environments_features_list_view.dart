import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/features/feature_value_cell.dart';

import 'feature_dashboard_constants.dart';
import 'tabs_bloc.dart';

class EnvironmentsAndFeatureValuesListView extends StatelessWidget {
  const EnvironmentsAndFeatureValuesListView({
    Key? key,
    required this.bloc,
  }) : super(key: key);

  final FeaturesOnThisTabTrackerBloc bloc;

  @override
  Widget build(BuildContext context) {
    final ScrollController controller = ScrollController();
    return StreamBuilder<CombinedEnvironmentInfoAndFeaturesEditing>(
        stream: bloc.environmentAndFeaturesCurrentlyEditingStream,
        builder: (context, snapshot) {
          final unselHeight = bloc.unselectedFeatureCountForHeight;
          final selHeight = bloc.selectedFeatureCountForHeight;
          if (bloc.features.isNotEmpty && snapshot.hasData) {
            print("event happened " + snapshot.data!.toString());
          }
          return Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: SizedBox(
              height: unselHeight + selHeight + headerHeight,
              child: ScrollConfiguration(
                behavior: CustomScrollBehavior(),
                child: Scrollbar(
                  controller: controller,
                  child: ListView(
                    controller: controller,
                    scrollDirection: Axis.horizontal,
                    physics: const ClampingScrollPhysics(),
                    children: [
                      if (bloc.features.isNotEmpty && snapshot.hasData)
                        ...snapshot.data!.environmentsInfo.shownEnvironments.map((efv) {
                          return PerEnvironmentFeatureWidget(bloc: bloc, environment: efv);
                        })
                    ],
                  ),
                ),
              ),
            ),
          );
        });
  }
}

class PerEnvironmentFeatureWidget extends StatelessWidget {
  final Environment environment;
  final FeaturesOnThisTabTrackerBloc bloc;

  const PerEnvironmentFeatureWidget({
    Key? key,
    required this.bloc, required Environment this.environment,
  }) : super(key: key);


  @override
  Widget build(BuildContext context) {
    final environmentFeatureValues = bloc.applicationFeatureValues.environments
        .firstWhere((env) => env.environmentId == environment.id, orElse: () => EnvironmentFeatureValues(environmentId: environment.id, environmentName: environment.name));

    return SizedBox(
      width: cellWidth,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Container(
            width: cellWidth,
            color: Theme.of(context).highlightColor,
            height: headerHeight,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(environment.name.toUpperCase(),
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context)
                        .textTheme
                        .overline!
                        .copyWith(
                            color: Theme.of(context)
                                        .brightness ==
                                    Brightness.light
                                ? Colors.black87
                                : null,
                            fontSize: 14)),
              ],
            ),
          ),
          ...bloc.features.map((f) {
            return Container(
              decoration: BoxDecoration(
                  border: Border(
                bottom: BorderSide(
                    color: Theme.of(context)
                        .buttonTheme
                        .colorScheme!
                        .onSurface
                        .withOpacity(0.12),
                    width: 1.0),
                right: BorderSide(
                    color: Theme.of(context)
                        .buttonTheme
                        .colorScheme!
                        .onSurface
                        .withOpacity(0.12),
                    width: 1.0),
              )),
              child: FeatureValueCell(
                  tabsBloc: bloc,
                  feature: f,
                  value: environmentFeatureValues.features.firstWhereOrNull(
                      (fv) => fv.key == f.key),
                  efv: environmentFeatureValues),
            );
          }).toList(),
        ],
      ),
    );
  }
}
