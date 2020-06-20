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

class TabsView extends StatelessWidget {
  final FeatureStatusFeatures featureStatus;

  const TabsView({Key key, this.featureStatus}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      initialIndex: 0,
      // The number of tabs / content sections to display.
      length: 3,
      child: Column(
        children: <Widget>[
          TabBar(
            isScrollable: false,
            labelStyle: Theme.of(context).textTheme.bodyText1,
            labelColor: Theme.of(context).textTheme.subtitle2.color,
            unselectedLabelColor: Theme.of(context).textTheme.bodyText2.color,
            tabs: [
              Tab(
                  text: 'FEATURE FLAGS',
                  icon:
                      Icon(Icons.flag, color: Theme.of(context).primaryColor)),
              Tab(
                  text: 'FEATURE VALUES',
                  icon:
                      Icon(Icons.code, color: Theme.of(context).primaryColor)),
              Tab(
                  text: 'CONFIGURATIONS',
                  icon: Icon(Icons.device_hub,
                      color: Theme.of(context).primaryColor)), //find JSON icon
            ],
          ),
          SizedBox(
            height: MediaQuery.of(context).size.height - 265,
            child: TabBarView(
              children: [
                //Environments
                SingleChildScrollView(
                    child: FeatureFlagAndEnvironmentsTable(
                        featureStatus: featureStatus, isFeature: true)),
                // Groups permissions
                SingleChildScrollView(
                    child: FeatureFlagAndEnvironmentsTable(
                        featureStatus: featureStatus, isFeature: false)),
                SingleChildScrollView(
                    child: FeatureFlagAndEnvironmentsTable(
                        featureStatus: featureStatus)),
                // Service accounts
              ],
            ),
          )
        ],
      ),
    );
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

    return Padding(
      padding: const EdgeInsets.only(top: 8.0),
      child: Column(children: [
        _FeatureHeader(
          featureStatuses: featureStatus,
        ),
        ...featureStatus.applicationFeatureValues.features
            .where((e) => isFeature != null
                ? (isFeature
                    ? e.valueType == FeatureValueType.BOOLEAN
                    : (e.valueType == FeatureValueType.NUMBER ||
                        e.valueType == FeatureValueType.STRING))
                : e.valueType == FeatureValueType.JSON)
            .map((e) => StreamBuilder<LineStatusFeature>(
                stream: bloc.getLineStatus(e.id),
                builder: (context, snapshot) {
                  if (!snapshot.hasData || snapshot.hasError) {
                    return SizedBox.shrink();
                  }

                  return _FeatureRepresentation(
                    feature: e,
                    applicationFeatureValues:
                        featureStatus.applicationFeatureValues,
                    lineStatus: snapshot.data,
                  );
                })),
      ]),
    );
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
