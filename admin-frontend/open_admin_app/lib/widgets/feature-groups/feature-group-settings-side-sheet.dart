import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/feature-groups/boolean_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_strategy_editing_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/features_drop_down.dart';
import 'package:open_admin_app/widgets/feature-groups/json_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/number_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/string_value_container_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/individual_strategy_bloc.dart';

class FeatureGroupSettings extends StatefulWidget {
  final FeatureGroupListGroup featureGroup;
  final FeatureGroupBloc bloc;

  const FeatureGroupSettings(
      {Key? key, required this.featureGroup, required this.bloc})
      : super(key: key);

  @override
  State<FeatureGroupSettings> createState() => _FeatureGroupSettingsState();
}

class _FeatureGroupSettingsState extends State<FeatureGroupSettings> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).canvasColor,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(children: [
            Align(
              alignment: Alignment.topLeft,
              child: IconButton(
                icon: const Icon(Icons.close, size: 24),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
            StreamBuilder<FeatureGroup>(
                stream: widget.bloc.featureGroupStream,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const FHLoadingIndicator();
                  } else if (snapshot.connectionState ==
                          ConnectionState.active ||
                      snapshot.connectionState == ConnectionState.done) {
                    if (snapshot.hasError) {
                      return const FHLoadingError();
                    } else if (snapshot.hasData) {
                      return Column(
                        children: [
                          Text(snapshot.data!.name,
                              style: Theme.of(context).textTheme.titleLarge),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              SelectableText.rich(TextSpan(
                                  style: DefaultTextStyle.of(context).style,
                                  children: [
                                    const TextSpan(
                                      text: 'Application: ',
                                    ),
                                    TextSpan(
                                      text: widget.bloc.featureGroupsBloc
                                          .currentApplicationsStream.value
                                          .firstWhere((app) =>
                                              app.id ==
                                              widget.bloc.featureGroupsBloc
                                                  .mrClient.currentAid)
                                          .name,
                                      style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.bold),
                                    ),
                                  ])),
                              SelectableText.rich(TextSpan(
                                  style: DefaultTextStyle.of(context).style,
                                  children: [
                                    const TextSpan(
                                      text: 'Environment: ',
                                    ),
                                    TextSpan(
                                      text: widget.featureGroup.environmentName,
                                      style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.bold),
                                    ),
                                  ])),
                            ],
                          ),
                          const SizedBox(height: 32.0),
                          Row(children: [
                            Expanded(
                              child: _FeaturesSettings(
                                featureGroup: snapshot.data!,
                                bloc: widget.bloc,
                              ),
                            ),
                            Expanded(
                              child: _StrategySettings(
                                featureGroup: snapshot.data!,
                                bloc: widget.bloc,
                              ),
                            )
                          ]),
                        ],
                      );
                    }
                  }
                  return const SizedBox.shrink();
                }),
            if (widget.bloc.featureGroupsBloc.envRoleTypeStream.value
                .contains(RoleType.CHANGE_VALUE))
              ButtonBar(
                alignment: MainAxisAlignment.center,
                children: [
                  FHFlatButtonTransparent(
                    title: 'Cancel',
                    keepCase: true,
                    onPressed: () {
                      Navigator.pop(context);
                    },
                  ),
                  FHFlatButton(
                    title: 'Apply all changes',
                    onPressed: () async {
                      await widget.bloc.saveFeatureGroupUpdates();
                      widget.bloc.featureGroupsBloc.mrClient.addSnackbar(Text(
                          'Settings for group "${widget.featureGroup.name}" have been updated'));
                      Navigator.pop(context);
                    },
                  )
                ],
              )
          ]),
        ),
      ),
    );
  }
}

class _StrategySettings extends StatelessWidget {
  final FeatureGroup featureGroup;
  final FeatureGroupBloc bloc;
  const _StrategySettings(
      {Key? key, required this.featureGroup, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    bool editable = bloc.featureGroupsBloc.envRoleTypeStream.value
        .contains(RoleType.CHANGE_VALUE);
    return StreamBuilder<FeatureGroupStrategy?>(
        stream: bloc.strategyStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextButton.icon(
                    label: Text(snapshot.data!.name),
                    icon: const Icon(Icons.call_split_outlined),
                    onPressed: () => showDialog(
                        context: context,
                        builder: (_) {
                          return AlertDialog(
                              title: Text(editable
                                  ? 'Edit split targeting rules'
                                  : 'View split targeting rules'),
                              content: BlocProvider(
                                creator: (c, b) => IndividualStrategyBloc(
                                    RolloutStrategy(
                                        name: snapshot.data!.name,
                                        attributes: snapshot.data!.attributes)),
                                child: FeatureGroupStrategyEditingWidget(
                                    bloc: bloc, editable: true),
                              ));
                        })),
                const SizedBox(height: 8.0),
                if (editable)
                  TextButton.icon(
                    onPressed: () {
                      bloc.removeStrategy(snapshot.data!);
                    },
                    icon: const Icon(
                      Icons.cancel,
                    ),
                    label: const Text("Remove strategy"),
                  )
              ],
            );
          } else {
            return TextButton.icon(
                label: const Text("Add rollout strategy"),
                icon: const Icon(Icons.call_split_outlined),
                onPressed: editable
                    ? () => showDialog(
                        context: context,
                        builder: (_) {
                          return AlertDialog(
                              title: Text(editable
                                  ? 'Edit split targeting rules'
                                  : 'View split targeting rules'),
                              content: BlocProvider(
                                creator: (c, b) =>
                                    IndividualStrategyBloc(RolloutStrategy(
                                  name: '',
                                  id: 'created',
                                )),
                                child: FeatureGroupStrategyEditingWidget(
                                    bloc: bloc, editable: true),
                              ));
                        })
                    : null);
          }
        });
  }
}

class _FeaturesSettings extends StatelessWidget {
  final FeatureGroup featureGroup;
  final FeatureGroupBloc bloc;
  const _FeaturesSettings(
      {Key? key, required this.featureGroup, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var editable = bloc.featureGroupsBloc.envRoleTypeStream.value
        .contains(RoleType.CHANGE_VALUE);
    return Column(
      mainAxisSize: MainAxisSize.max,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text("Features List", style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        if (editable)
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              StreamBuilder<List<FeatureGroupFeature>>(
                  stream: bloc.availableFeaturesStream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                      return FeaturesDropDown(
                          features: snapshot.data!, bloc: bloc);
                    } else {
                      return const SizedBox.shrink();
                    }
                  }),
              const SizedBox(
                width: 8.0,
              ),
              TextButton.icon(
                  icon: const Icon(Icons.add),
                  label: const Text('Add Feature'),
                  onPressed: () => {_addFeatureToGroup(bloc)}),
            ],
          ),
        StreamBuilder<List<FeatureGroupFeature>>(
            stream: bloc.groupFeaturesStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                return Column(
                  children: [
                    for (FeatureGroupFeature feature in snapshot.data!)
                      Padding(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8.0, vertical: 16.0),
                        child: Card(
                          child: Padding(
                            padding: const EdgeInsets.all(8.0),
                            child: SizedBox(
                              height: 42,
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      Text(feature.name),
                                      const SizedBox(width: 8.0),
                                      FeatureValueContainer(
                                          bloc: bloc, feature: feature),
                                    ],
                                  ),
                                  if (editable)
                                    IconButton(
                                        onPressed: () {
                                          bloc.removeFeatureFromGroup(feature);
                                        },
                                        icon: const Icon(
                                            Icons.delete_forever_sharp),
                                        color: Theme.of(context)
                                            .colorScheme
                                            .primary)
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                  ],
                );
              } else {
                return const SizedBox.shrink();
              }
            })
      ],
    );
  }

  _addFeatureToGroup(FeatureGroupBloc bloc) {
    bloc.addFeatureToGroup();
  }
}

class FeatureValueContainer extends StatelessWidget {
  final FeatureGroupFeature feature;
  final FeatureGroupBloc bloc;

  const FeatureValueContainer(
      {Key? key, required this.bloc, required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final editable = bloc.featureGroupsBloc.envRoleTypeStream.value
        .contains(RoleType.CHANGE_VALUE);
    switch (feature.type) {
      case FeatureValueType.STRING:
        return EditFeatureGroupStringValueContainer(
          editable: editable,
          feature: feature,
          bloc: bloc,
        );
      case FeatureValueType.BOOLEAN:
        return EditFeatureGroupBooleanValueWidget(
          editable: editable,
          feature: feature,
          bloc: bloc,
        );
      case FeatureValueType.NUMBER:
        return EditFeatureGroupNumberValueContainer(
          editable: editable,
          feature: feature,
          bloc: bloc,
        );
      case FeatureValueType.JSON:
        return EditFeatureGroupJsonValueContainer(
          editable: editable,
          feature: feature,
          bloc: bloc,
        );
      default:
        "foo";
    }
    return const SizedBox.shrink();
  }
}
