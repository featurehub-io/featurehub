import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/theme/custom_text_style.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_info_card.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/application_strategies_dropdown.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/retire_feature_value_checkbox_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_add.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/strategy_card.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class EditFeatureValueWidget extends StatefulWidget {
  final EditingFeatureValueBloc bloc;

  const EditFeatureValueWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  State<EditFeatureValueWidget> createState() => _EditFeatureValueWidgetState();
}

class _EditFeatureValueWidgetState extends State<EditFeatureValueWidget> {
  bool sortToggle = true;
  int sortColumnIndex = 0;
  bool _isHistoryPresent = false;

  @override
  Widget build(BuildContext context) {
    final roles = widget.bloc.environmentFeatureValue.roles;
    final canSave = roles.contains(RoleType.CHANGE_VALUE) ||
        roles.contains(RoleType.LOCK) ||
        roles.contains(RoleType.UNLOCK);

    return StreamBuilder<List<RolloutStrategy>>(
        stream: widget.bloc.strategies,
        builder: (streamCtx, strategiesLatest) {
          return Column(
            children: [
              StreamBuilder<bool>(
                  stream: widget.bloc.isFeatureValueUpdatedStream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData && snapshot.data! && canSave) {
                      return SizedBox(
                        height: 46.0,
                        child: Container(
                          color:
                              Theme.of(context).snackBarTheme.backgroundColor,
                          child: Row(children: [
                            const SizedBox(
                              width: 8.0,
                            ),
                            const Text('You have unsaved changes, save?'),
                            TextButton(
                                style: TextButton.styleFrom(
                                  foregroundColor: Theme.of(context)
                                      .snackBarTheme
                                      .actionTextColor,
                                ),
                                onPressed: () {
                                  Navigator.pop(context); //close the side panel
                                },
                                child: const Text("Cancel")),
                            FilledButton(
                                onPressed: () async {
                                  try {
                                    Navigator.pop(
                                        context); //close the side panel
                                    await widget.bloc.saveFeatureValueUpdates();
                                    widget.bloc.perApplicationFeaturesBloc
                                        .mrClient
                                        .addSnackbar(Text(
                                            'Feature ${widget.bloc.feature.name.toUpperCase()} '
                                            'in the environment ${widget.bloc.environmentFeatureValue.environmentName.toUpperCase()} has been updated!'));
                                  } catch (e, s) {
                                    widget.bloc.perApplicationFeaturesBloc
                                        .mrClient
                                        .dialogError(e, s);
                                  }
                                },
                                child: const Text("Save")),
                          ]),
                        ),
                      );
                    } else {
                      return const SizedBox.shrink();
                    }
                  }),
              Expanded(
                child: SingleChildScrollView(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              widget.bloc.feature.name,
                              style: CustomTextStyle.bodyMediumBold(context),
                            ),
                            IconButton(
                              icon: const Icon(Icons.close, size: 20),
                              onPressed: () => Navigator.of(context).pop(),
                            ),
                          ],
                        ),
                      ),
                      const FHPageDivider(),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              "Environment",
                              style: CustomTextStyle.bodySmallLight(context),
                            ),
                            Text(widget
                                .bloc.environmentFeatureValue.environmentName),
                          ],
                        ),
                      ),
                      Card(
                        elevation: 3.0,
                        shadowColor: Colors.transparent,
                        child: Padding(
                          padding: const EdgeInsets.only(
                              top: 8.0, bottom: 16.0, left: 8.0, right: 8.0),
                          child: Column(
                            children: [
                              const SizedBox(height: 16.0),
                              Row(
                                children: [
                                  Text(
                                    "Locked status",
                                    style:
                                        CustomTextStyle.bodySmallLight(context),
                                  ),
                                  const SizedBox(
                                    width: 4.0,
                                  ),
                                  const FHInfoCardWidget(
                                      message:
                                          "Locking mechanism provides an additional safety for feature changes when deploying incomplete code to production."
                                          " Locked status prevents any changes to default value, "
                                          "strategies, strategy values and 'retired' status. "
                                          "Typically, developers keep features locked "
                                          "to indicate they are not ready to be turned on for testers, product owners, customers and other stakeholders."),
                                ],
                              ),
                              const SizedBox(height: 4.0),
                              LockUnlockSwitch(
                                environmentFeatureValue:
                                    widget.bloc.environmentFeatureValue,
                                fvBloc: widget.bloc,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16.0),
                      StreamBuilder<FeatureValue>(
                          stream: widget.bloc.currentFv,
                          builder: (context, featureValueLatest) {
                            if (featureValueLatest.hasData) {
                              final canChangeValue = widget
                                  .bloc.environmentFeatureValue.roles
                                  .contains(RoleType.CHANGE_VALUE);
                              var editable = !featureValueLatest.data!.locked &&
                                  canChangeValue;
                              List<Widget> strategyWidgets = [];
                              if (strategiesLatest.hasData) {
                                strategyWidgets = strategiesLatest.data!
                                    .map((RolloutStrategy strategy) {
                                  return Column(
                                    children: [
                                      StrategyCard(
                                          key: ValueKey(strategy),
                                          strBloc: widget.bloc,
                                          rolloutStrategy: strategy,
                                          featureValueType:
                                              widget.bloc.feature.valueType),
                                    ],
                                  );
                                }).toList();
                              }
                              return Card(
                                elevation: 3.0,
                                shadowColor: Colors.transparent,
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 8.0, vertical: 24.0),
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text("Default value",
                                          style: CustomTextStyle.bodySmallLight(
                                              context)),
                                      StrategyCard(
                                          strBloc: widget.bloc,
                                          featureValueType:
                                              widget.bloc.feature.valueType),
                                      const SizedBox(height: 16.0),
                                      Row(
                                        children: [
                                          Text("Strategy variations",
                                              style: CustomTextStyle
                                                  .bodySmallLight(context)),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "Add a strategy variation to serve a value other than default. "
                                                  "You can change strategies evaluation order by dragging and dropping the cards below. "
                                                  "Strategies are evaluated in order from top to bottom. Evaluation stops when it hits a matching strategy."
                                                  " 'Group Strategy' evaluation comes last. If no strategies match, then 'default' feature value is served."),
                                          const SizedBox(
                                            width: 8.0,
                                          ),
                                          if (editable)
                                            AddStrategyButton(
                                              bloc: widget.bloc,
                                            ),
                                        ],
                                      ),
                                      if (strategyWidgets.isEmpty)
                                        const Text("No strategies set"),
                                      buildReorderableListView(
                                        strategyWidgets,
                                        featureValueLatest,
                                        canChangeValue,
                                        widget.bloc,
                                        strategiesLatest: strategiesLatest,
                                      ),
                                      const SizedBox(height: 24.0),
                                      Row(
                                        children: [
                                          Text("Group strategy variations",
                                              style: CustomTextStyle
                                                  .bodySmallLight(context)),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "Feature groups are recommended when you want to set the same strategy for multiple features in the same environment. "
                                                  "Feature group strategy can be created and edited from the Feature Groups page.")
                                        ],
                                      ),
                                      if (featureValueLatest
                                              .data
                                              ?.featureGroupStrategies
                                              ?.isEmpty ==
                                          true)
                                        const Text("No group strategies set"),
                                      if (featureValueLatest
                                              .data
                                              ?.featureGroupStrategies
                                              ?.isNotEmpty ==
                                          true)
                                        for (var groupStrategy
                                            in featureValueLatest
                                                .data!.featureGroupStrategies!)
                                          StrategyCard(
                                              groupRolloutStrategy:
                                                  groupStrategy,
                                              strBloc: widget.bloc,
                                              featureValueType: widget
                                                  .bloc.feature.valueType),
                                      const SizedBox(height: 24.0),
                                      Row(
                                        children: [
                                          Text(
                                              "Application strategy variations",
                                              style: CustomTextStyle
                                                  .bodySmallLight(context)),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "Application strategies are created at application level and can be assigned to multiple features in any environment. "
                                                  "Application strategy can be created and edited from the Application Strategies page.")
                                        ],
                                      ),
                                      StreamBuilder<
                                              List<RolloutStrategyInstance>>(
                                          stream:
                                              widget.bloc.applicationStrategies,
                                          builder: (context, snapshot) {
                                            if (snapshot.hasData &&
                                                snapshot.data != null &&
                                                snapshot.data!.isNotEmpty) {
                                              strategyWidgets = snapshot.data!
                                                  .map((RolloutStrategyInstance
                                                      strategy) {
                                                return Column(
                                                  children: [
                                                    StrategyCard(
                                                        key: ValueKey(strategy),
                                                        strBloc: widget.bloc,
                                                        applicationRolloutStrategy:
                                                            strategy,
                                                        featureValueType: widget
                                                            .bloc
                                                            .feature
                                                            .valueType),
                                                  ],
                                                );
                                              }).toList();
                                              return buildReorderableListView(
                                                  strategyWidgets,
                                                  featureValueLatest,
                                                  canChangeValue,
                                                  widget.bloc,
                                                  appStrategiesLatest:
                                                      snapshot);
                                            }
                                            return const Text(
                                                "No application strategies set");
                                          }),
                                      if (editable)
                                        TextButton(
                                            onPressed: () => widget.bloc
                                                .getApplicationStrategies(),
                                            child: const Text(
                                                "Show available app strategies")),
                                      StreamBuilder<
                                              List<
                                                  ListApplicationRolloutStrategyItem>>(
                                          stream: widget.bloc
                                              .availableApplicationStrategies,
                                          builder: (context, snapshot) {
                                            if (snapshot.hasData &&
                                                snapshot.data != null &&
                                                snapshot.data!.isNotEmpty) {
                                              return Row(
                                                mainAxisAlignment:
                                                    MainAxisAlignment.start,
                                                children: [
                                                  ApplicationStrategiesDropDown(
                                                    strategies: snapshot.data!,
                                                    bloc: widget.bloc,
                                                  ),
                                                  const SizedBox(
                                                    width: 8.0,
                                                  ),
                                                  TextButton.icon(
                                                      icon:
                                                          const Icon(Icons.add),
                                                      label: const Text(
                                                          'Add Strategy'),
                                                      onPressed: () => {
                                                            widget.bloc
                                                                .addApplicationStrategy()
                                                          }),
                                                ],
                                              );
                                            } else {
                                              return const SizedBox.shrink();
                                            }
                                          }),
                                      const SizedBox(height: 24.0),
                                      Row(
                                        children: [
                                          Text(
                                            "Retired status",
                                            style:
                                                CustomTextStyle.bodySmallLight(
                                                    context),
                                          ),
                                          const SizedBox(
                                            width: 4.0,
                                          ),
                                          const FHInfoCardWidget(
                                              message:
                                                  "When feature flag is not needed any longer in your application,"
                                                  " and ready to be removed, you can first 'retire' this feature in a given environment"
                                                  " to test how your application behaves. This means that the feature won't be visible by the SDKs,"
                                                  " imitating the 'deleted' state. You can uncheck the box to 'un-retire' a feature if you change your mind"
                                                  " as this operation is reversible. Once you retire feature values across all the environments"
                                                  "  and test that your application behaves as expected, you can delete your entire feature.")
                                        ],
                                      ),
                                      RetireFeatureValueCheckboxWidget(
                                          environmentFeatureValue: widget
                                              .bloc.environmentFeatureValue,
                                          fvBloc: widget.bloc,
                                          editable: editable,
                                          retired: widget.bloc
                                              .currentFeatureValue.retired),
                                      //this is where we need to pass retired from the actual value
                                      const SizedBox(height: 16.0),
                                      FeatureValueUpdatedByCell(
                                        strBloc: widget.bloc,
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            } else {
                              return const SizedBox.shrink();
                            }
                          }),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _isHistoryPresent
                              ? TextButton(
                                  onPressed: () {
                                    widget.bloc.clearHistory();
                                    setState(() {
                                      _isHistoryPresent = false;
                                    });
                                  },
                                  child: const Text("Hide actions"))
                              : TextButton(
                                  onPressed: () {
                                    widget.bloc.getHistory();
                                    setState(() {
                                      _isHistoryPresent = true;
                                    });
                                  },
                                  child: const Text("Show actions")),
                          StreamBuilder<FeatureHistoryItem?>(
                              stream: widget.bloc.featureHistoryListSource,
                              builder: (context, snapshot) {
                                if (snapshot.hasData) {
                                  FeatureHistoryItem item = snapshot.data!;
                                  return Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Padding(
                                        padding: const EdgeInsets.all(8.0),
                                        child: Text("Showing last 20",
                                            style:
                                                CustomTextStyle.bodySmallLight(
                                                    context)),
                                      ),
                                      Card(
                                          child: SingleChildScrollView(
                                        scrollDirection: Axis.horizontal,
                                        child: SelectionArea(
                                          child: DataTable(
                                            dataRowMinHeight: 36,
                                            dataRowMaxHeight: double.infinity,
                                            showCheckboxColumn: false,
                                            sortAscending: sortToggle,
                                            sortColumnIndex: sortColumnIndex,
                                            columns: [
                                              DataColumn(
                                                  label: const Text(
                                                      'Timestamp (UTC)'),
                                                  onSort:
                                                      (columnIndex, ascending) {
                                                    onSortColumn(
                                                        snapshot.data!.history,
                                                        columnIndex,
                                                        ascending);
                                                  }),
                                              DataColumn(
                                                  label: const Text('Name'),
                                                  onSort:
                                                      (columnIndex, ascending) {
                                                    onSortColumn(
                                                        snapshot.data!.history,
                                                        columnIndex,
                                                        ascending);
                                                  }),
                                              DataColumn(
                                                  label: const Text('Email'),
                                                  onSort:
                                                      (columnIndex, ascending) {
                                                    onSortColumn(
                                                        snapshot.data!.history,
                                                        columnIndex,
                                                        ascending);
                                                  }),
                                              DataColumn(
                                                label: const Text('Type'),
                                                onSort:
                                                    (columnIndex, ascending) {
                                                  onSortColumn(
                                                      snapshot.data!.history,
                                                      columnIndex,
                                                      ascending);
                                                },
                                              ),
                                              DataColumn(
                                                label:
                                                    const Text('Default Value'),
                                                onSort:
                                                    (columnIndex, ascending) {
                                                  onSortColumn(
                                                      snapshot.data!.history,
                                                      columnIndex,
                                                      ascending);
                                                },
                                              ),
                                              DataColumn(
                                                label: const Text('Locked'),
                                                onSort:
                                                    (columnIndex, ascending) {
                                                  onSortColumn(
                                                      snapshot.data!.history,
                                                      columnIndex,
                                                      ascending);
                                                },
                                              ),
                                              DataColumn(
                                                label: const Text('Retired'),
                                                onSort:
                                                    (columnIndex, ascending) {
                                                  onSortColumn(
                                                      snapshot.data!.history,
                                                      columnIndex,
                                                      ascending);
                                                },
                                              ),
                                              const DataColumn(
                                                label:
                                                    Text('Rollout Strategies'),
                                              ),
                                            ],
                                            rows: [
                                              for (FeatureHistoryValue value
                                                  in item.history)
                                                DataRow(cells: [
                                                  DataCell(Text(DateFormat(
                                                          'yyyy-MM-dd HH:mm:ss')
                                                      .format(value.when))),
                                                  DataCell(
                                                    Text(value.who.name),
                                                  ),
                                                  DataCell(
                                                    Text(value.who.type ==
                                                            PersonType.person
                                                        ? value.who.email ?? ''
                                                        : ''),
                                                  ),
                                                  DataCell(Text(
                                                      value.who.type ==
                                                              PersonType.person
                                                          ? 'User'
                                                          : 'Service Account')),
                                                  DataCell(ConstrainedBox(
                                                    constraints:
                                                        const BoxConstraints(
                                                            maxWidth: 300),
                                                    child: Text(
                                                        value.value.toString()),
                                                  )),
                                                  DataCell(Text(value.locked
                                                      ? "true"
                                                      : 'false')),
                                                  DataCell(Text(value.retired
                                                      ? "true"
                                                      : 'false')),
                                                  DataCell(Column(
                                                    mainAxisAlignment:
                                                        MainAxisAlignment.start,
                                                    crossAxisAlignment:
                                                        CrossAxisAlignment
                                                            .start,
                                                    children: [
                                                      for (var i in value
                                                          .rolloutStrategies)
                                                        ConstrainedBox(
                                                          constraints:
                                                              const BoxConstraints(
                                                                  maxWidth:
                                                                      500),
                                                          child: Wrap(
                                                            crossAxisAlignment:
                                                                WrapCrossAlignment
                                                                    .center,
                                                            children: [
                                                              Text(
                                                                  '${i.name} = ${i.value}'),
                                                              TextButton(
                                                                  onPressed: () =>
                                                                      showDialog(
                                                                          context:
                                                                              context,
                                                                          builder:
                                                                              (_) {
                                                                            return AlertDialog(
                                                                                content: Column(
                                                                                  crossAxisAlignment: CrossAxisAlignment.start,
                                                                                  mainAxisSize: MainAxisSize.min,
                                                                                  children: [
                                                                                    Text(
                                                                                      "Strategy Rules",
                                                                                      style: Theme.of(context).textTheme.titleLarge,
                                                                                    ),
                                                                                    if (i.attributes != null) SelectableText('${i.attributes?.join("\n")}'),
                                                                                    const SizedBox(
                                                                                      height: 16.0,
                                                                                    ),
                                                                                    if (i.percentage != null)
                                                                                      Text(
                                                                                        "Percentage Rollout",
                                                                                        style: Theme.of(context).textTheme.titleLarge,
                                                                                      ),
                                                                                    if (i.percentage != null) Text('${i.percentage! / 10000}'),
                                                                                  ],
                                                                                ),
                                                                                actions: <Widget>[
                                                                                  FHFlatButton(
                                                                                    title: 'OK',
                                                                                    onPressed: () {
                                                                                      Navigator.pop(context);
                                                                                    },
                                                                                  )
                                                                                ]);
                                                                          }),
                                                                  child:
                                                                      const Text(
                                                                          "more"))
                                                            ],
                                                          ),
                                                        )
                                                    ],
                                                  )),
                                                ])
                                            ],
                                          ),
                                        ),
                                      ))
                                    ],
                                  );
                                } else {
                                  return const SizedBox.shrink();
                                }
                              }),
                        ],
                      )
                    ])),
              ),
            ],
          );
        });
  }

  ReorderableListView buildReorderableListView(
      List<Widget> widgets,
      AsyncSnapshot<FeatureValue> featureValueLatest,
      bool canChangeValue,
      EditingFeatureValueBloc bloc,
      {AsyncSnapshot<List<RolloutStrategy>>? strategiesLatest,
      AsyncSnapshot<List<RolloutStrategyInstance>>? appStrategiesLatest}) {
    return ReorderableListView(
      shrinkWrap: true,
      buildDefaultDragHandles: false,
      children: <Widget>[
        for (Widget wid in widgets)
          ReorderableDragStartListener(
              key: ValueKey(wid),
              enabled: !featureValueLatest.data!.locked && canChangeValue,
              index: widgets.indexOf(wid),
              child: wid)
      ],
      onReorder: (int oldIndex, int newIndex) {
        if (newIndex > oldIndex) {
          newIndex -= 1;
        }
        if (strategiesLatest != null) {
          final items = strategiesLatest.data![oldIndex];

          strategiesLatest.data!
            ..removeWhere((element) => element.id == items.id)
            ..insert(newIndex, items);

          widget.bloc.updateStrategyValue();
        } else if (appStrategiesLatest != null) {
          final items = appStrategiesLatest.data![oldIndex];

          appStrategiesLatest.data!
            ..removeWhere((element) => element.strategyId == items.strategyId)
            ..insert(newIndex, items);

          widget.bloc.updateApplicationStrategyValue();
        }
      },
    );
  }

  void onSortColumn(List<FeatureHistoryValue> featureHistoryValue,
      int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          featureHistoryValue.sort((a, b) {
            return a.when.compareTo(b.when);
          });
        } else {
          featureHistoryValue.sort((a, b) {
            return b.when.compareTo(a.when);
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          featureHistoryValue.sort((a, b) =>
              a.who.name.toLowerCase().compareTo(b.who.name.toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) =>
              b.who.name.toLowerCase().compareTo(a.who.name.toLowerCase()));
        }
      }
      if (columnIndex == 2) {
        if (ascending) {
          featureHistoryValue.sort((a, b) =>
              a.who.email!.toLowerCase().compareTo(b.who.email!.toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) =>
              b.who.email!.toLowerCase().compareTo(a.who.email!.toLowerCase()));
        }
      }
      if (columnIndex == 3) {
        if (ascending) {
          featureHistoryValue.sort((a, b) => a.who.type
              .toString()
              .toLowerCase()
              .compareTo(b.who.type.toString().toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) => b.who.type
              .toString()
              .toLowerCase()
              .compareTo(a.who.type.toString().toLowerCase()));
        }
      }
      if (columnIndex == 4) {
        if (ascending) {
          featureHistoryValue.sort((a, b) => a.value
              .toString()
              .toLowerCase()
              .compareTo(b.value.toString().toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) => b.value
              .toString()
              .toLowerCase()
              .compareTo(a.value.toString().toLowerCase()));
        }
      }
      if (columnIndex == 5) {
        if (ascending) {
          featureHistoryValue.sort((a, b) =>
              a.locked.toString().compareTo(b.locked.toString().toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) =>
              b.locked.toString().compareTo(a.locked.toString().toLowerCase()));
        }
      }
      if (columnIndex == 6) {
        if (ascending) {
          featureHistoryValue.sort((a, b) => a.retired
              .toString()
              .compareTo(b.retired.toString().toLowerCase()));
        } else {
          featureHistoryValue.sort((a, b) => b.retired
              .toString()
              .compareTo(a.retired.toString().toLowerCase()));
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }
}
