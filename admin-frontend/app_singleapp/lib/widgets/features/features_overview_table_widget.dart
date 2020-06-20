import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_boolean.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_json.dart';
import 'package:app_singleapp/widgets/features/sdk_details_dialog.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_status_bloc.dart';
import 'feature_value_row_locked.dart';
import 'feature_value_row_number.dart';
import 'feature_value_row_string.dart';
import 'feature_value_status_tags.dart';
import 'feature_values_bloc.dart';

final _log = Logger('FeaturesOverviewTable');

class FeaturesOverviewTableWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    try {
      return StreamBuilder<FeatureStatusFeatures>(
          stream: bloc.appFeatureValues,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return SizedBox.shrink();
            }

            if (snapshot.hasData &&
                snapshot.data.sortedByNameEnvironmentIds.isEmpty) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  NoEnvironmentMessage(),
                ],
              );
            }
            if (snapshot.hasData &&
                snapshot.data.applicationFeatureValues.features.isEmpty) {
              return NoFeaturesMessage();
            }
            return FHCardWidget(
                child: TabsView(featureStatus: snapshot.data),
                width: double.infinity);
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return SizedBox.shrink();
    }
  }
}

enum _TabsState { FLAGS, VALUES, CONFIGURATIONS }

class _TabsBloc implements Bloc {
  final FeatureStatusFeatures featureStatus;
  final _stateSource = BehaviorSubject<_TabsState>.seeded(_TabsState.FLAGS);
  List<Feature> _featuresForTabs;
  final _hiddenEnvironments = <String>{}; // set of strings
  final _hiddenEnvironmentsSource =
      BehaviorSubject<Set<String>>.seeded(<String>{});
  final _featureCurrentlyEditingSource =
      BehaviorSubject<Set<String>>.seeded(<String>{});

  // determine which tab they have selected
  Stream<_TabsState> get currentTab => _stateSource.stream;
  // which environments are hidden
  Stream<Set<String>> get hiddenEnvironments =>
      _hiddenEnvironmentsSource.stream;
  Stream<Set<String>> get featureCurrentlyEditingStream =>
      _featureCurrentlyEditingSource.stream;
  List<Feature> get features => _featuresForTabs;

  _TabsBloc(this.featureStatus) : assert(featureStatus != null) {
    _fixFeaturesForTabs(_stateSource.value);
  }

  void _fixFeaturesForTabs(_TabsState tab) {
    _featuresForTabs =
        featureStatus.applicationFeatureValues.features.where((f) {
      return ((tab == _TabsState.FLAGS) &&
              f.valueType == FeatureValueType.BOOLEAN) ||
          ((tab == _TabsState.VALUES) &&
              (f.valueType == FeatureValueType.NUMBER ||
                  f.valueType == FeatureValueType.STRING)) ||
          ((tab == _TabsState.CONFIGURATIONS) &&
              (f.valueType == FeatureValueType.JSON));
    }).toList();
  }

  void swapTab(_TabsState tab) {
    _fixFeaturesForTabs(tab);
    _stateSource.add(tab);
  }

  void hideEnvironment(String envId) {
    if (!_hiddenEnvironments.contains(envId)) {
      _hiddenEnvironments.add(envId);
      _hiddenEnvironmentsSource.add(_hiddenEnvironments);
    }
  }

  List<EnvironmentFeatureValues> get sortedEnvironmentsThatAreShowing {
    return featureStatus.sortedByNameEnvironmentIds
        .where((id) => !_hiddenEnvironments.contains(id))
        .map((id) => featureStatus.applicationEnvironments[id])
        .toList();
  }

  @override
  void dispose() {}

  void hideOrShowFeature(Feature feature) {
    final val = _featureCurrentlyEditingSource.value;

    if (!val.contains(feature.key)) {
      val.add(feature.key);
    } else {
      val.remove(feature.key);
    }

    _featureCurrentlyEditingSource.add(val);
  }
}

class TabsView extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;

  const TabsView({Key key, this.featureStatus}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
        creator: (_c, _b) => _TabsBloc(featureStatus),
        child: Column(
          children: [
            _FeatureTabsHeader(),
            _FeatureTableWithHiddenList(),
            _FeatureTabsBodyHolder(),
          ],
        ));
  }
}

class _FeatureTableWithHiddenList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<_TabsBloc>(context);
    return Container(
      height: _headerHeight,
      child: Row(
        children: [
          Text('Hidden environments'),
          Flexible(
            child: Container(
              height: _headerHeight,
              child: StreamBuilder<Set<String>>(
                  stream: bloc.hiddenEnvironments,
                  builder: (context, snapshot) {
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      children: [
                        if (snapshot.hasData)
                          ...snapshot.data
                              .map((e) => _FeatureTabEnvironmentWithCheck(
                                  name: bloc
                                      .featureStatus
                                      .applicationEnvironments[e]
                                      .environmentName))
                              .toList(),
                      ],
                    );
                  }),
            ),
          ),
        ],
      ),
    );
  }
}

class _FeatureTabEnvironmentWithCheck extends StatelessWidget {
  final String name;
  final String envId;

  const _FeatureTabEnvironmentWithCheck({Key key, this.name, this.envId})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Checkbox(
          value: true,
        ),
        Text(name),
      ],
    );
  }
}

final _unselectedRowHeight = 60.0;
final _selectedRowHeight = 200.0;
final _headerHeight = 80.0;

class _FeatureTabsBodyHolder extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<_TabsBloc>(context);

    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        StreamBuilder<_TabsState>(
            stream: bloc.currentTab,
            builder: (context, snapshot) {
              return Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: [
                    Container(height: _headerHeight, child: Text('')),
                    ...bloc.features.map(
                      (f) {
                        return _FeatureTabFeatureNameCollapsed(
                            tabsBloc: bloc, feature: f);
                      },
                    ).toList(),
                  ]);
            }),
        Flexible(
          child: Container(
            height: 600.0,
            child: _FeatureTabEnvironments(bloc: bloc),
          ),
        )
      ],
    );
  }
}

class _FeatureTabEnvironments extends StatelessWidget {
  const _FeatureTabEnvironments({
    Key key,
    @required this.bloc,
  }) : super(key: key);

  final _TabsBloc bloc;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: bloc.hiddenEnvironments,
        builder: (context, snapshot) {
          return StreamBuilder<_TabsState>(
              stream: bloc.currentTab,
              builder: (context, snapshot) {
                return ListView(
                  scrollDirection: Axis.horizontal,
                  children: [
                    if (bloc.features.isNotEmpty)
                      ...bloc.sortedEnvironmentsThatAreShowing.map((efv) {
                        return Container(
                          width: 100.0,
                          child: Column(
                            children: [
                              Container(
                                height: _headerHeight,
                                child: Column(
                                  children: [
                                    Checkbox(
                                      value: true,
                                    ),
                                    Text(
                                      efv.environmentName,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              ...bloc.features
                                  .map((f) => efv.features.firstWhere(
                                      (fv) => fv.key == f.key,
                                      orElse: () => FeatureValue()))
                                  .map((fv) {
                                return (fv.key == null)
                                    ? SizedBox.shrink()
                                    : _FeatureTabFeatureValue(
                                        tabsBloc: bloc, value: fv);
                              }).toList(),
                            ],
                          ),
                        );
                      })
                  ],
                );
              });
        });
  }
}

class _FeatureTabFeatureValue extends StatelessWidget {
  final _TabsBloc tabsBloc;
  final FeatureValue value;

  const _FeatureTabFeatureValue({Key key, this.tabsBloc, this.value})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(value.key));
          return Container(
              height: amSelected ? _selectedRowHeight : _unselectedRowHeight,
              child: Text(value.valueBoolean.toString()));
        });
  }
}

class _FeatureTabFeatureNameCollapsed extends StatelessWidget {
  final _TabsBloc tabsBloc;
  final Feature feature;

  const _FeatureTabFeatureNameCollapsed(
      {Key key, @required this.tabsBloc, @required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(feature.key));
          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => tabsBloc.hideOrShowFeature(feature),
            child: Container(
                padding: EdgeInsets.fromLTRB(0, 8, 0, 8),
                height: amSelected ? _selectedRowHeight : _unselectedRowHeight,
                width: 200.0,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Icon(
                      amSelected
                          ? Icons.keyboard_arrow_down
                          : Icons.keyboard_arrow_right,
                      size: 24.0,
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(left: 8.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text('${feature.name}',
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.bodyText1),
                            Text(
                                '${feature.valueType.toString().split('.').last}',
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                    fontFamily: 'Source', fontSize: 12)),
                          ],
                        ),
                      ),
                    ),
                  ],
                )),
          );
        });
  }
}

class _FeatureTabsHeader extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _FeatureTab(
            text: 'FEATURE FLAGS', icon: Icons.flag, state: _TabsState.FLAGS),
        _FeatureTab(
            text: 'FEATURE VALUES', icon: Icons.code, state: _TabsState.VALUES),
        _FeatureTab(
            text: 'CONFIGURATIONS',
            icon: Icons.device_hub,
            state: _TabsState.CONFIGURATIONS),
      ],
    );
  }
}

class _FeatureTab extends StatelessWidget {
  final String text;
  final IconData icon;
  final _TabsState state;

  const _FeatureTab(
      {Key key, @required this.text, @required this.icon, @required this.state})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<_TabsBloc>(context);

    return StreamBuilder<_TabsState>(
        stream: bloc.currentTab,
        builder: (context, snapshot) {
          return GestureDetector(
            onTap: () {
              print("swapped to $state");
              bloc.swapTab(state);
            },
            child: Container(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  Icon(this.icon, color: Theme.of(context).primaryColor),
                  Text(text,
                      style: Theme.of(context).textTheme.bodyText1.copyWith(
                          color: state == snapshot.data
                              ? Theme.of(context).textTheme.subtitle2.color
                              : Theme.of(context).textTheme.bodyText2.color))
                ],
              ),
            ),
          );
        });
  }
}

class FeatureFlagAndEnvironmentsTable extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;
  final bool isFeature;

  const FeatureFlagAndEnvironmentsTable({
    Key key,
    @required this.featureStatus,
    this.isFeature,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    final maxEnvironments = 4;

    return Column(
      children: [
        ...featureStatus.applicationFeatureValues.features.map((f) {
          return Container(
            height: 120.0,
            child: Row(
              children: [
                Column(
                  children: [
                    Container(width: 200.0, child: Text('Feature ${f.name}')),
                  ],
                ),
                Flexible(
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    children: [
                      ...featureStatus.sortedByNameEnvironmentIds.map((eId) {
                        return Container(
                          width: 100.0,
                          padding: const EdgeInsets.all(8.0),
                          color: Colors.black12,
                          child: Text(
                              'Environment ${featureStatus.applicationEnvironments[eId].environmentName}'),
                        );
                      })
                    ],
                  ),
                ),
              ],
            ),
          );
        })
      ],
    );
//    return Padding(
//      padding: const EdgeInsets.only(top: 8.0),
//      child: Column(children: [
//        _FeatureHeader(
//          featureStatuses: featureStatus,
//        ),
//        ...featureStatus.applicationFeatureValues.features
//            .where((e) => isFeature != null
//                ? (isFeature
//                    ? e.valueType == FeatureValueType.BOOLEAN
//                    : (e.valueType == FeatureValueType.NUMBER ||
//                        e.valueType == FeatureValueType.STRING))
//                : e.valueType == FeatureValueType.JSON)
//            .map((e) => StreamBuilder<LineStatusFeature>(
//                stream: bloc.getLineStatus(e.id),
//                builder: (context, snapshot) {
//                  if (!snapshot.hasData || snapshot.hasError) {
//                    return SizedBox.shrink();
//                  }
//
//                  return _FeatureRepresentation(
//                    feature: e,
//                    applicationFeatureValues:
//                        featureStatus.applicationFeatureValues,
//                    lineStatus: snapshot.data,
//                  );
//                })),
//      ]),
//    );
  }
}

class _FeatureHeader extends StatelessWidget {
  final FeatureStatusFeatures featureStatuses;

  const _FeatureHeader({Key key, this.featureStatuses}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    return Table(
      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
      border: TableBorder.symmetric(
          outside:
              BorderSide(width: 1.0, color: Theme.of(context).dividerColor)),
      children: [_header(context, bloc)],
    );
  }

  TableRow _header(BuildContext context, FeatureStatusBloc bloc) {
    return TableRow(
        decoration: BoxDecoration(
          color: Theme.of(context).selectedRowColor,
        ),
        children:
            headerRow(featureStatuses.applicationFeatureValues, context, bloc));
  }

  List<Widget> headerRow(ApplicationFeatureValues appFeatureValues,
      BuildContext context, FeatureStatusBloc bloc) {
    final hrow = <Widget>[];

    hrow.add(SizedBox.shrink()); // placebolder for name + action

    if (appFeatureValues.environments.isNotEmpty) {
      appFeatureValues.environments.asMap().forEach((index, efv) {
        hrow.add(Padding(
          padding: const EdgeInsets.only(left: 14.0),
          child: Container(
              child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Text(efv.environmentName,
                  style: Theme.of(context).textTheme.bodyText1),
              SDKDetailsWidget(bloc: bloc, envId: efv.environmentId)
            ],
          )),
        ));
      });
    }
    return hrow;
  }
}

class _FeatureRepresentation extends StatefulWidget {
  final Feature feature;
  final LineStatusFeature lineStatus;
  final ApplicationFeatureValues applicationFeatureValues;

  const _FeatureRepresentation(
      {Key key,
      @required this.feature,
      this.lineStatus,
      @required this.applicationFeatureValues})
      : assert(feature != null),
        assert(applicationFeatureValues != null),
        super(key: key);

  @override
  __FeatureRepresentationState createState() => __FeatureRepresentationState();
}

class __FeatureRepresentationState extends State<_FeatureRepresentation> {
  bool _open = false;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    final featureValuesThisFeature = widget.lineStatus.environmentFeatureValues
        .expand((e) => e.features.where((fv) => fv.key == widget.feature.key))
        .toList();

    if (_open) {
      return BlocProvider(
          creator: (_c, _b) => FeatureValuesBloc(
              bloc.applicationId,
              widget.feature,
              bloc.mrClient,
              featureValuesThisFeature,
              widget.applicationFeatureValues),
          child: Builder(
              // need to use this so we get access to the bloc provider we just created
              builder: (sbContext) => Table(
                    defaultVerticalAlignment: TableCellVerticalAlignment.middle,
                    border: TableBorder.symmetric(
                        outside: BorderSide(
                            color: Theme.of(context).buttonColor,
                            width: 2.0,
                            style: BorderStyle.solid)),
                    children: [
                      generateCell(bloc),
                      ...bearMeChildren(sbContext)
                    ],
                  )));
    }

    return Table(
      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
      border: TableBorder.symmetric(
          outside:
              BorderSide(width: 1.0, color: Theme.of(context).dividerColor)),
      children: [
        generateCell(bloc),
      ],
    );
  }

  List<TableRow> bearMeChildren(BuildContext context) {
    final rows = [
      FeatureValueEditLocked.build(context, widget.lineStatus, widget.feature)
    ];

    TableRow editor;

    switch (widget.feature.valueType) {
      case FeatureValueType.STRING:
        editor = FeatureValueEditString.build(
            context, widget.lineStatus, widget.feature);
        break;
      case FeatureValueType.NUMBER:
        editor = FeatureValueEditNumber.build(
            context, widget.lineStatus, widget.feature);
        break;
      case FeatureValueType.BOOLEAN:
        editor = FeatureValueEditBoolean.build(
            context, widget.lineStatus, widget.feature);
        break;
      case FeatureValueType.JSON:
        editor = FeatureValueEditJson.build(
            context, widget.lineStatus, widget.feature);
        break;
    }

    if (editor != null) {
      rows.add(editor);
    }

    rows.addAll([
      FeatureValueUpdatedBy.build(context, widget.lineStatus, widget.feature),
      FeatureValueActions.build(context, widget.lineStatus, widget.feature, () {
        setState(() => _open = !_open);
        RefreshFeatureNotification(featureId: widget.feature.id)
          ..dispatch(context);
      })
    ]);

    return rows;
  }

  TableRow generateCell(FeatureStatusBloc bloc) {
    final feature = widget.feature;

    final row = <Widget>[];
    row.add(GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => setState(() => _open = !_open),
      child: Container(
          padding: EdgeInsets.fromLTRB(15, 8, 0, 10),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: <Widget>[
              Icon(
                _open ? Icons.keyboard_arrow_down : Icons.keyboard_arrow_right,
                size: 24.0,
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(left: 8.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text('${feature.name}',
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.bodyText1),
                      Text('${feature.valueType.toString().split('.').last}',
                          style: TextStyle(fontFamily: 'Source', fontSize: 12)),
                    ],
                  ),
                ),
              ),
            ],
          )),
    ));

    widget.lineStatus.sortedByNameEnvironmentIds.forEach((envId) {
      final efv = widget.lineStatus.applicationEnvironments[envId];

      FeatureValue fv;

      // see if there is a value for this feature in this environment
      if (efv != null && efv.features.isNotEmpty) {
        fv = efv.features.firstWhere((featureValue) {
          return featureValue.key == feature.key;
        }, orElse: () => null);
      }

      Widget cellWidget;
      if (feature.valueType == FeatureValueType.BOOLEAN) {
        cellWidget = _BooleanStatusLabel(
          fv: fv,
          efv: efv,
          feature: feature,
        );
      } else {
        cellWidget = _ConfigurationStatusLabel(
          fv: fv,
          efv: efv,
          feature: feature,
        );
      }

      row.add(GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: () => setState(() => _open = !_open),
          child: cellWidget));
    });

    return TableRow(children: row);
  }
}

class _ConfigurationStatusLabel extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const _ConfigurationStatusLabel(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      if (fv != null && fv.isSet(feature)) {
        return _ConfigurationValueContainer(feature: feature, fv: fv);
      } else {
        return _NotSetContainer();
      }
    }
    if (fv == null && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class _ConfigurationValueContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const _ConfigurationValueContainer(
      {Key key, @required this.feature, @required this.fv})
      : assert(fv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
          constraints: BoxConstraints(maxWidth: 100),
          padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.all(Radius.circular(16.0)),
            color: Colors.lightBlue,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Flexible(
                fit: FlexFit.tight,
                flex: 4,
                child: Text(_getValue(),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                    style: GoogleFonts.openSans(
                        textStyle: Theme.of(context).primaryTextTheme.button)),
              ),
              fv.locked
                  ? Flexible(
                      fit: FlexFit.tight,
                      flex: 1,
                      child: Padding(
                        padding: const EdgeInsets.only(left: 4.0, top: 2.0),
                        child: Icon(
                          Icons.lock_outline,
                          color: Colors.black54,
                          size: 12.0,
                        ),
                      ),
                    )
                  : Container(),
            ],
          )),
    );
  }

  String _getValue() {
    switch (feature.valueType) {
      case FeatureValueType.STRING:
        return fv.valueString ?? '';
      case FeatureValueType.NUMBER:
        return fv.valueNumber?.toString() ?? '';
      case FeatureValueType.BOOLEAN:
        return ''; // shouldn't happen
      case FeatureValueType.JSON:
        return fv.valueJson.replaceAll('\n', '') ?? '';
    }

    return '';
  }
}

class _BooleanStatusLabel extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const _BooleanStatusLabel(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles != null && efv.roles.isNotEmpty) {
      if (fv != null && fv.isSet(feature)) {
        return _BooleanContainer(feature, fv);
      } else {
        return _NotSetContainer();
      }
    }
    if (fv == null && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class _NotSetContainer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: Container(
              padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.all(Radius.circular(16.0)),
                border: Border.all(
                  color: Theme.of(context).disabledColor,
                  width: 1,
                ),
              ),
              child: Text('not set',
                  style: GoogleFonts.openSans(
                      textStyle: Theme.of(context).textTheme.bodyText1))),
        ),
      ],
    );
  }
}

class _BooleanContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  _BooleanContainer(this.feature, this.fv);

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: Container(
              padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.all(Radius.circular(16.0)),
                color: fv.valueBoolean ? Color(0xff11C8B5) : Color(0xffF44C49),
              ),
              child: Row(
                children: <Widget>[
                  Text(fv.valueBoolean ? 'ON' : 'OFF',
                      style: GoogleFonts.openSans(
                          textStyle:
                              Theme.of(context).primaryTextTheme.button)),
                  fv.locked
                      ? Padding(
                          padding: const EdgeInsets.only(left: 4.0, top: 2.0),
                          child: Icon(
                            Icons.lock_outline,
                            color: Colors.black54,
                            size: 12.0,
                          ),
                        )
                      : Container(),
                ],
              )),
        ),
      ],
    );
  }
}

class NoEnvironmentMessage extends StatelessWidget {
  const NoEnvironmentMessage({
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);

    return Row(
      children: <Widget>[
        Text(
            'Either there are no environments defined for this application or you don\'t have permissions to access any of them',
            style: Theme.of(context).textTheme.caption),
        StreamBuilder<ReleasedPortfolio>(
            stream: bloc.mrClient.personState.isCurrentPortfolioOrSuperAdmin,
            builder: (context, snapshot) {
              if (snapshot.hasData &&
                  snapshot.data.currentPortfolioOrSuperAdmin) {
                return FHFlatButtonTransparent(
                    title: 'Manage application',
                    keepCase: true,
                    onPressed: () => ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/manage-app',
                                transition: TransitionType.material,
                                params: {
                              'id': [bloc.applicationId],
                              'tab-name': ['environments']
                            }));
              } else {
                return Container();
              }
            })
      ],
    );
  }
}

class NoFeaturesMessage extends StatelessWidget {
  const NoFeaturesMessage({
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(top: 16.0),
      child: Column(
        children: <Widget>[
          Text('There are no features defined for this application',
              style: Theme.of(context).textTheme.caption),
        ],
      ),
    );
  }
}
