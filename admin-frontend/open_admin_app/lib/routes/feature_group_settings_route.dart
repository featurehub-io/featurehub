import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_accent.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/feature-groups/boolean_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_strategy_provider.dart';
import 'package:open_admin_app/widgets/feature-groups/features_drop_down.dart';
import 'package:open_admin_app/widgets/feature-groups/json_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/number_value_container_widget.dart';
import 'package:open_admin_app/widgets/feature-groups/string_value_container_widget.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class FeatureGroupSettingsRoute extends StatefulWidget {
  const FeatureGroupSettingsRoute({Key? key}) : super(key: key);

  @override
  State<FeatureGroupSettingsRoute> createState() =>
      _FeatureGroupSettingsRouteState();
}

class _FeatureGroupSettingsRouteState extends State<FeatureGroupSettingsRoute> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureGroupBloc>(context);
    return Container(
      color: Theme.of(context).canvasColor,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(children: [
            StreamBuilder<FeatureGroup>(
                stream: bloc.featureGroupStream,
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
                          Row(
                            children: [
                              SelectableText.rich(TextSpan(
                                  style: DefaultTextStyle.of(context).style,
                                  children: [
                                    const TextSpan(
                                      text: 'Group: ',
                                    ),
                                    TextSpan(
                                      text: snapshot.data!.name,
                                      style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.bold),
                                    ),
                                  ])),
                              const SizedBox(width: 16.0),
                              StreamBuilder<List<Application>>(
                                  stream: bloc.featureGroupsBloc
                                      .currentApplicationsStream,
                                  builder: (context, snapshot) {
                                    if (snapshot.hasData &&
                                        snapshot.data!.isNotEmpty) {
                                      return SelectableText.rich(TextSpan(
                                          style: DefaultTextStyle.of(context)
                                              .style,
                                          children: [
                                            const TextSpan(
                                              text: 'Application: ',
                                            ),
                                            TextSpan(
                                              text: snapshot.data
                                                  ?.firstWhere((app) =>
                                                      app.id ==
                                                      bloc.applicationId)
                                                  .name,
                                              style: const TextStyle(
                                                  fontSize: 14,
                                                  fontWeight: FontWeight.bold),
                                            ),
                                          ]));
                                    } else {
                                      return const SizedBox.shrink();
                                    }
                                  }),
                              const SizedBox(
                                width: 16.0,
                              ),
                              SelectableText.rich(TextSpan(
                                  style: DefaultTextStyle.of(context).style,
                                  children: [
                                    const TextSpan(
                                      text: 'Environment: ',
                                    ),
                                    TextSpan(
                                      text: bloc.featureGroupStream.value
                                          .environmentName,
                                      style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.bold),
                                    ),
                                  ])),
                            ],
                          ),
                          const SizedBox(
                            height: 8.0,
                          ),
                          const FHPageDivider(),
                          const SizedBox(
                            height: 8.0,
                          ),
                          StreamBuilder(
                              stream: bloc.featureGroupsBloc.envRoleTypeStream,
                              builder: (context, snapshot) {
                                if (snapshot.hasData &&
                                    snapshot.data!
                                        .contains(RoleType.CHANGE_VALUE)) {
                                  return StreamBuilder<bool>(
                                      stream: bloc.isGroupUpdatedStream,
                                      builder: (context, snapshot) {
                                        if (snapshot.hasData &&
                                            snapshot.data!) {
                                          return ButtonBar(
                                            alignment: MainAxisAlignment.end,
                                            children: [
                                              FHFlatButtonTransparent(
                                                title: 'Cancel',
                                                keepCase: true,
                                                onPressed: () {
                                                  ManagementRepositoryClientBloc
                                                      .router
                                                      .navigateTo(context,
                                                          '/feature-groups');
                                                },
                                              ),
                                              FHFlatButtonAccent(
                                                title: 'Apply all changes',
                                                keepCase: true,
                                                onPressed: () async {
                                                  await bloc
                                                      .saveFeatureGroupUpdates();
                                                  bloc.featureGroupsBloc
                                                      .mrClient
                                                      .addSnackbar(Text(
                                                          'Settings for group "${bloc.featureGroupStream.value.name}" have been updated'));
                                                },
                                              )
                                            ],
                                          );
                                        } else {
                                          return const SizedBox.shrink();
                                        }
                                      });
                                } else {
                                  return const Text("No permissions");
                                }
                              }),
                          const SizedBox(height: 32.0),
                          Row(children: [
                            Expanded(
                              child: Container(
                                decoration: BoxDecoration(
                                    border: Border(
                                        right: BorderSide(
                                            color: Theme.of(context)
                                                .colorScheme
                                                .onPrimaryContainer,
                                            width: 0.5))),
                                child: _FeaturesSettings(
                                  featureGroup: snapshot.data!,
                                  bloc: bloc,
                                ),
                              ),
                            ),
                            Expanded(
                              child: _StrategySettings(
                                featureGroup: snapshot.data!,
                                bloc: bloc,
                              ),
                            )
                          ]),
                        ],
                      );
                    }
                  }
                  return const SizedBox.shrink();
                }),
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
    return StreamBuilder<GroupRolloutStrategy?>(
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
                              content: BlocProvider.builder(
                                creator: (c, b) {
                                  var rs = snapshot.data!;
                                  return StrategyEditorBloc(rs.toEditing(),
                                      GroupRolloutStrategyProvider(bloc));
                                },
                                builder: (c, b) => StrategyEditingWidget(
                                    bloc: b, editable: true),
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
                              content: BlocProvider.builder(
                                creator: (c, b) => StrategyEditorBloc(
                                    EditingRolloutStrategy.newStrategy(),
                                    GroupRolloutStrategyProvider(bloc)),
                                builder: (c, b) => StrategyEditingWidget(
                                    bloc: b, editable: true),
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
        Text("Features List", style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 16),
        if (editable)
          Row(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              StreamBuilder<List<FeatureGroupFeature>>(
                  stream: bloc.availableFeaturesStream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                      return Expanded(
                        child: FeaturesDropDown(
                            features: snapshot.data!, bloc: bloc),
                      );
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
        const SizedBox(height: 8.0),
        StreamBuilder<List<FeatureGroupFeature>>(
            stream: bloc.groupFeaturesStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                return Column(
                  children: [
                    for (FeatureGroupFeature feature in snapshot.data!)
                      Padding(
                        padding: const EdgeInsets.fromLTRB(6.0, 8.0, 8.0, 8.0),
                        child: Card(
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            side: BorderSide(
                              color: Theme.of(context).colorScheme.outline,
                            ),
                            borderRadius:
                                const BorderRadius.all(Radius.circular(12)),
                          ),
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
                                      if (feature.locked)
                                        Tooltip(
                                            message:
                                                "Feature value is locked. Unlock from the main Features dashboard to enable editing",
                                            child: Icon(Icons.lock_outline,
                                                size: 14.0,
                                                color: Theme.of(context)
                                                    .iconTheme
                                                    .color
                                                    ?.withOpacity(0.8))),
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
