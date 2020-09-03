import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

// represents the editing of the states of a single boolean flag on a single environment
// function for dirty callback needs to be added
class FeatureValueBooleanEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;
  final RolloutStrategy rolloutStrategy;
  final _CustomStrategyBloc strBloc;

  FeatureValueBooleanEnvironmentCell(
      {Key key,
      this.environmentFeatureValue,
      this.feature,
      this.fvBloc,
      this.rolloutStrategy,
      this.strBloc})
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);

  @override
  _FeatureValueBooleanEnvironmentCellState createState() =>
      _FeatureValueBooleanEnvironmentCellState();
}

class _FeatureValueBooleanEnvironmentCellState
    extends State<FeatureValueBooleanEnvironmentCell> {
  String featureOn;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canWrite = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          if (snap.hasData) {
            return Padding(
              padding: const EdgeInsets.only(left: 16.0, right: 16.0),
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    Text(
                        widget.rolloutStrategy == null
                            ? 'default'
                            : widget.rolloutStrategy.name,
                        style: Theme.of(context).textTheme.caption),
                    SizedBox(
                      width: 4.0,
                    ),
                    DropdownButton(
                      items: <String>['On', 'Off']
                          .map<DropdownMenuItem<String>>((String value) {
                        return DropdownMenuItem<String>(
                          value: value,
                          child: Text(
                            value,
                            style: Theme.of(context).textTheme.bodyText2,
                          ),
                        );
                      }).toList(),
                      value: featureOn,
                      onChanged: snap.data == false && canWrite
                          ? (value) {
                              if (widget.rolloutStrategy == null) {
                                widget.fvBloc.dirty(
                                    widget.environmentFeatureValue
                                        .environmentId, (current) {
                                  current.value = (value == 'On');
                                });
                              } else {
                                widget.rolloutStrategy.value = (value == 'On');
                                widget.strBloc.markDirty();
                              }
                              setState(() {
                                featureOn = value;
                              });
                            }
                          : null,
                      disabledHint: Text(
                          widget.featureValue.locked ? 'Locked' : 'No access',
                          style: Theme.of(context).textTheme.caption),
                    ),
                  ]),
            );
          }
          return SizedBox.shrink();
        });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (widget.rolloutStrategy == null) {
      featureOn = widget.featureValue.valueBoolean ? 'On' : 'Off';
    } else {
      featureOn = widget.rolloutStrategy.value ? 'On' : 'Off';
    }
  }
}

class _CustomStrategyBloc extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  final _strategySource =
      BehaviorSubject<List<RolloutStrategy>>.seeded(<RolloutStrategy>[]);

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  _CustomStrategyBloc(this.environmentFeatureValue, this.feature, this.fvBloc)
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId) {
    _strategySource.add(featureValue.rolloutStrategies);
  }

  void markDirty() {
    fvBloc.dirty(environmentFeatureValue.environmentId, (current) {
      current.customStrategies = _strategySource.value;
    });
  }

  // call from + Add Strategy to add one
  void addStrategy(RolloutStrategy rs) {
    final strategies = _strategySource.value;
    strategies.add(rs);
    markDirty();
    _strategySource.add(strategies);
  }

  @override
  void dispose() {}
}

class FeatureValueBooleanCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueBooleanCellEditor(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            FeatureValueEditLockedCell(
              environmentFeatureValue: environmentFeatureValue,
              feature: feature,
              fvBloc: fvBloc,
            ),
            FeatureValueBooleanEnvironmentCell(
              environmentFeatureValue: environmentFeatureValue,
              feature: feature,
              fvBloc: fvBloc,
            ),
            BlocProvider(
              creator: (_c, _b) =>
                  _CustomStrategyBloc(environmentFeatureValue, feature, fvBloc),
              child: Builder(
                builder: (ctx) {
                  final strategyBloc =
                      BlocProvider.of<_CustomStrategyBloc>(ctx);

                  return Column(
                    children: [
                      StreamBuilder<List<RolloutStrategy>>(
                          stream: strategyBloc.strategies,
                          builder: (streamCtx, snap) {
                            if (snap.hasData) {
                              return Container(
                                  child: Column(
                                children: [
                                  for (RolloutStrategy strategy in snap.data)
                                    FeatureValueBooleanEnvironmentCell(
                                      environmentFeatureValue:
                                          environmentFeatureValue,
                                      feature: feature,
                                      fvBloc: fvBloc,
                                      strBloc: strategyBloc,
                                      rolloutStrategy: strategy,
                                    ),
                                ],
                              ));

                              // render your strategies and stuff here, have them remove
                              // themselves from the parent bloc and each  time it does so
                              // it needs to trigger "dirty" call in fvBloc
                            } else {
                              return Container();
                            }
                          }),
                      _AddStrategyButton()
                    ],
                  );
                },
              ), // need to put custom strategies here, trigger dirty each time change something
            ),
          ],
        ),
        FeatureValueUpdatedByCell(
          environmentFeatureValue: environmentFeatureValue,
          feature: feature,
          fvBloc: fvBloc,
        ),
      ],
    );
  }
}

class _AddStrategyButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return FHFlatButtonTransparent(
      title: '+ Add strategy',
      keepCase: true,
      onPressed: () {
        BlocProvider.of<_CustomStrategyBloc>(context)
            .addStrategy(RolloutStrategy());
      },
    );
  }
}
