import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_boolean.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_json.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_status_bloc.dart';
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
                child: TabsView(
                  featureStatus: snapshot.data,
                  applicationId: bloc.applicationId,
                  bloc: bloc,
                ),
                width: double.infinity);
          });
    } catch (e, s) {
      _log.shout('Failed to render, $e\n$s\n');
      return SizedBox.shrink();
    }
  }
}

enum TabsState { FLAGS, VALUES, CONFIGURATIONS }

class _TabsBloc implements Bloc {
  final String applicationId;
  FeatureStatusFeatures featureStatus;
  final _stateSource = BehaviorSubject<TabsState>.seeded(TabsState.FLAGS);
  List<Feature> _featuresForTabs;
  final _hiddenEnvironments = <String>{}; // set of strings
  final _hiddenEnvironmentsSource =
      BehaviorSubject<Set<String>>.seeded(<String>{});
  final _currentlyEditingFeatureKeys = <String>{};
  final _featureCurrentlyEditingSource = BehaviorSubject<Set<String>>();
  final ManagementRepositoryClientBloc mrClient;
  final _allFeaturesByKey = <String, Feature>{};

  // determine which tab they have selected
  Stream<TabsState> get currentTab => _stateSource.stream;
  // which environments are hidden
  Stream<Set<String>> get hiddenEnvironments =>
      _hiddenEnvironmentsSource.stream;
  Stream<Set<String>> get featureCurrentlyEditingStream =>
      _featureCurrentlyEditingSource.stream;
  List<Feature> get features => _featuresForTabs;
  final featureValueBlocs = <String, FeatureValuesBloc>{};
  final FeatureStatusBloc featureStatusBloc;
  StreamSubscription<FeatureStatusFeatures> _featureStream;
  StreamSubscription<Feature> _newFeatureStream;

  _TabsBloc(this.featureStatus, this.applicationId, this.mrClient,
      this.featureStatusBloc)
      : assert(featureStatus != null),
        assert(featureStatusBloc != null),
        assert(applicationId != null) {
    _featureCurrentlyEditingSource.add(_currentlyEditingFeatureKeys);

    // if they have created a new feature we want to swap to the right tab
    _fixFeaturesForTabs(_stateSource.value);
    _refixFeaturesByKey();

    _featureStream = featureStatusBloc.appFeatureValues.listen((appFeatures) {
      featureStatus = appFeatures;

      _fixFeaturesForTabs(_stateSource.value);
      _refixFeaturesByKey();

      _checkForFeaturesWeWereEditingThatHaveNowGone();
    });

    _newFeatureStream =
        featureStatusBloc.publishNewFeatureStream.listen((feature) {
      switch (feature.valueType) {
        case FeatureValueType.BOOLEAN:
          _stateSource.value = TabsState.FLAGS;
          break;
        case FeatureValueType.STRING:
          _stateSource.value = TabsState.VALUES;
          break;
        case FeatureValueType.NUMBER:
          _stateSource.value = TabsState.VALUES;
          break;
        case FeatureValueType.JSON:
          _stateSource.value = TabsState.CONFIGURATIONS;
          break;
      }

      _currentlyEditingFeatureKeys.add(feature.key);
    });
  }

  // turns them into a map for easy access
  void _refixFeaturesByKey() {
    featureStatus.applicationFeatureValues.features.forEach((f) {
      _allFeaturesByKey[f.key] = f;
    });
  }

  void _fixFeaturesForTabs(TabsState tab) {
    _featuresForTabs =
        featureStatus.applicationFeatureValues.features.where((f) {
      return ((tab == TabsState.FLAGS) &&
              f.valueType == FeatureValueType.BOOLEAN) ||
          ((tab == TabsState.VALUES) &&
              (f.valueType == FeatureValueType.NUMBER ||
                  f.valueType == FeatureValueType.STRING)) ||
          ((tab == TabsState.CONFIGURATIONS) &&
              (f.valueType == FeatureValueType.JSON));
    }).toList();
  }

  void swapTab(TabsState tab) {
    _fixFeaturesForTabs(tab);
    _stateSource.add(tab);
  }

  void hideEnvironment(String envId) {
    if (!_hiddenEnvironments.contains(envId)) {
      _hiddenEnvironments.add(envId);
    } else {
      _hiddenEnvironments.remove(envId);
    }

    _hiddenEnvironmentsSource.add(_hiddenEnvironments);
  }

  List<EnvironmentFeatureValues> get sortedEnvironmentsThatAreShowing {
    return featureStatus.sortedByNameEnvironmentIds
        .where((id) => !_hiddenEnvironments.contains(id))
        .map((id) => featureStatus.applicationEnvironments[id])
        .toList();
  }

  @override
  void dispose() {
    // clean up any outstanding value blocs
    featureValueBlocs.values.forEach((element) => element.dispose());
    _featureStream.cancel();
    _newFeatureStream.cancel();
  }

  void hideOrShowFeature(Feature feature) {
    final val = _currentlyEditingFeatureKeys;

    if (!val.contains(feature.key)) {
      _createFeatureValueBlocForFeature(feature);
      val.add(feature.key);
    } else {
      final featureValueBloc = featureValueBlocs[feature.key];
      if (featureValueBloc != null) {
        featureValueBloc.dispose();
      }
      featureValueBlocs.remove(featureValueBloc);
      val.remove(feature.key);
    }

    _featureCurrentlyEditingSource.add(val);
  }

  void _createFeatureValueBlocForFeature(Feature feature) {
    List<FeatureValue> vals = featureStatus
        .applicationFeatureValues.environments
        .where((env) => !_hiddenEnvironments.contains(env.environmentId))
        .map((env) => env.features
            .firstWhere((fv) => fv.key == feature.key, orElse: () => null))
        .where((fv) => fv != null)
        .toList();

    featureValueBlocs[feature.key] = FeatureValuesBloc(applicationId, feature,
        mrClient, vals, featureStatus.applicationFeatureValues);
  }

  Feature findFeature(String key, String environmentId) {
    return _allFeaturesByKey[key];
  }

  void _checkForFeaturesWeWereEditingThatHaveNowGone() {
    final removeMissing = _currentlyEditingFeatureKeys;

    removeMissing.where((key) {
      final remove = _allFeaturesByKey[key] == null;
      if (remove) {
        // get rid of the block
        featureValueBlocs[key].dispose();
        featureValueBlocs.remove(key);
      }
      return remove;
    });

    // now make sure all features are there as
    // when we create one we can add it to the list and it not have a bloc
    removeMissing.forEach((key) {
      if (featureValueBlocs[key] == null) {
        _createFeatureValueBlocForFeature(_allFeaturesByKey[key]);
      }
    });

    // trigger a rebuild
    _featureCurrentlyEditingSource.add(removeMissing);
  }
}

class TabsView extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;
  final String applicationId;
  final FeatureStatusBloc bloc;

  const TabsView(
      {Key key,
      @required this.featureStatus,
      @required this.applicationId,
      @required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
        creator: (_c, _b) => _TabsBloc(featureStatus, applicationId,
            BlocProvider.of<ManagementRepositoryClientBloc>(context), bloc),
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
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text('Hidden environments'),
          ),
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
                          ..._sortedEnvironments(bloc, snapshot.data)
                              .map((env) {
                            return _FeatureTabEnvironmentWithCheck(
                                rowLayout: true,
                                envId: env.environmentId,
                                name: env.environmentName);
                          }).toList(),
                      ],
                    );
                  }),
            ),
          ),
        ],
      ),
    );
  }

  List<EnvironmentFeatureValues> _sortedEnvironments(
      _TabsBloc bloc, Set<String> envIds) {
    final envs = bloc.featureStatus.applicationFeatureValues.environments
        .where((e) => envIds.contains(e.environmentId))
        .toList();
    envs.sort((e1, e2) => e1.environmentName.compareTo(e2.environmentName));
    return envs;
  }
}

class _FeatureTabEnvironmentWithCheck extends StatelessWidget {
  final String name;
  final String envId;
  final bool rowLayout;

  const _FeatureTabEnvironmentWithCheck(
      {Key key, this.name, this.envId, bool rowLayout})
      : this.rowLayout = rowLayout ?? false,
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (rowLayout) {
      return ActionChip(
        avatar: CircleAvatar(
          backgroundColor: Colors.white,
          child: Icon(Icons.keyboard_arrow_down),
        ),
        label: Text(name),
        onPressed: () {
          BlocProvider.of<_TabsBloc>(context).hideEnvironment(envId);
        },
      );
    }

    return ActionChip(
      avatar: CircleAvatar(
        backgroundColor: Colors.white,
        child: Icon(Icons.keyboard_arrow_up),
      ),
      label: Text(
        name,
        overflow: TextOverflow.ellipsis,
      ),
      onPressed: () {
        BlocProvider.of<_TabsBloc>(context).hideEnvironment(envId);
      },
    );
  }
}

final _unselectedRowHeight = 60.0;
final _selectedRowHeight = 230.0;
final _headerHeight = 80.0;

class _FeatureTabsBodyHolder extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<_TabsBloc>(context);

    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        StreamBuilder<TabsState>(
            stream: bloc.currentTab,
            builder: (context, snapshot) {
              return Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: [
                    Container(
                        height: _headerHeight,
                        child: Padding(
                          padding: const EdgeInsets.only(top: 16.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Padding(
                                padding: const EdgeInsets.all(6.0),
                                child: Text('Environments'),
                              ),
                              Icon(Icons.keyboard_arrow_right)
                            ],
                          ),
                        )),
                    ...bloc.features.map(
                      (f) {
                        return _FeatureTabFeatureNameCollapsed(
                            tabsBloc: bloc, feature: f);
                      },
                    ).toList(),
                  ]);
            }),
        Flexible(
          child: _FeatureTabEnvironments(bloc: bloc),
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
          return StreamBuilder<TabsState>(
              stream: bloc.currentTab,
              builder: (context, snapshot) {
                return StreamBuilder<Set<String>>(
                    stream: bloc.featureCurrentlyEditingStream,
                    builder: (context, snapshot) {
                      return Container(
                        height: ((bloc.features.length -
                                    (snapshot.data?.length ?? 0)) *
                                _unselectedRowHeight) +
                            ((snapshot.data?.length ?? 0) *
                                _selectedRowHeight) +
                            _headerHeight,
                        child: ListView(
                          scrollDirection: Axis.horizontal,
                          children: [
                            if (bloc.features.isNotEmpty)
                              ...bloc.sortedEnvironmentsThatAreShowing
                                  .map((efv) {
                                return Container(
                                  width: snapshot.data == TabsState.FLAGS
                                      ? 100.0
                                      : 170.0,
                                  child: Column(
                                    children: [
                                      Container(
                                        height: _headerHeight,
                                        child: Column(
                                          children: [
                                            _FeatureTabEnvironmentWithCheck(
                                              name: efv.environmentName,
                                              envId: efv.environmentId,
                                            ),
                                          ],
                                        ),
                                      ),
                                      ...bloc.features.map((f) {
                                        return _FeatureTabFeatureValue(
                                            tabsBloc: bloc,
                                            feature: f,
                                            value: efv.features.firstWhere(
                                                (fv) => fv.key == f.key,
                                                orElse: () => null),
                                            efv: efv);
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

class _FeatureTabFeatureValue extends StatelessWidget {
  final _TabsBloc tabsBloc;
  final FeatureValue value;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const _FeatureTabFeatureValue(
      {Key key,
      @required this.tabsBloc,
      @required this.value,
      @required this.efv,
      this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(feature.key));

          Widget cellWidget;
          if (!amSelected) {
            if (feature.valueType == FeatureValueType.BOOLEAN) {
              cellWidget = _BooleanStatusLabel(
                fv: value,
                efv: efv,
                feature: feature,
              );
            } else {
              cellWidget = _ConfigurationStatusLabel(
                fv: value,
                efv: efv,
                feature: feature,
              );
            }

            cellWidget = GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => tabsBloc.hideOrShowFeature(feature),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    cellWidget,
                  ],
                ));
          } else if (feature == null) {
            cellWidget = Text('confused');
          } else {
            final fvBloc = tabsBloc.featureValueBlocs[feature.key];
            switch (feature.valueType) {
              case FeatureValueType.BOOLEAN:
                cellWidget = FeatureValueBooleanCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.STRING:
                cellWidget = FeatureValueStringCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.NUMBER:
                cellWidget = FeatureValueNumberCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.JSON:
                cellWidget = FeatureValueJsonCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
            }
          }

          return Container(
              height: amSelected ? _selectedRowHeight : _unselectedRowHeight,
              child: cellWidget);
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
                width: amSelected ? 200.0 : 200.0,
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
                        child: Container(
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
                              if (amSelected)
                                FeatureValueNameCell(feature: feature),
                              if (amSelected)
                                FeatureEditDeleteCell(feature: feature)
                            ],
                          ),
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
            text: 'FEATURE FLAGS', icon: Icons.flag, state: TabsState.FLAGS),
        _FeatureTab(
            text: 'FEATURE VALUES', icon: Icons.code, state: TabsState.VALUES),
        _FeatureTab(
            text: 'CONFIGURATIONS',
            icon: Icons.device_hub,
            state: TabsState.CONFIGURATIONS),
      ],
    );
  }
}

class _FeatureTab extends StatelessWidget {
  final String text;
  final IconData icon;
  final TabsState state;

  const _FeatureTab(
      {Key key, @required this.text, @required this.icon, @required this.state})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<_TabsBloc>(context);

    return StreamBuilder<TabsState>(
        stream: bloc.currentTab,
        builder: (context, snapshot) {
          return GestureDetector(
            onTap: () {
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
      if (fv != null && fv.id != null && fv.isSet(feature)) {
        return _ConfigurationValueContainer(feature: feature, fv: fv);
      } else {
        return _NotSetContainer();
      }
    }
    if ((fv == null || fv.id == null) && efv.roles.isEmpty) {
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
