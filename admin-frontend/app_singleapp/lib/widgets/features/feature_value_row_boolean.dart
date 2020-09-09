import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/features/create-strategy-widget.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

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
  final CustomStrategyBloc strBloc;

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
            return SizedBox(
              height: 50,
              child: Card(
                color: widget.rolloutStrategy == null
                    ? Color(0xffeee6ff)
                    : Color(0xfff2fde4),
                child: Padding(
                  padding: const EdgeInsets.only(left: 8.0, right: 2.0),
                  child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Expanded(
                            flex: 5,
                            child: widget.rolloutStrategy == null
                                ? Text('default',
                                    style: Theme.of(context).textTheme.caption)
                                : FHUnderlineButton(
                                    enabled: !snap.data && canWrite,
                                    title: widget.rolloutStrategy.name,
                                    onPressed: !snap.data && canWrite
                                        ? () => {
                                              widget.fvBloc.mrClient.addOverlay(
                                                  (BuildContext context) {
                                                //return null;
                                                return CreateValueStrategyWidget(
                                                  fvBloc: widget.fvBloc,
                                                  bloc: widget.strBloc,
                                                  rolloutStrategy:
                                                      widget.rolloutStrategy,
                                                );
                                              })
                                            }
                                        : null)),
                        Expanded(
                          flex: 3,
                          child: DropdownButtonHideUnderline(
                            child: DropdownButton(
                              items: <String>[
                                'On',
                                'Off'
                              ].map<DropdownMenuItem<String>>((String value) {
                                return DropdownMenuItem<String>(
                                  value: value,
                                  child: Text(
                                    value,
                                    style:
                                        Theme.of(context).textTheme.bodyText2,
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
                                        widget.rolloutStrategy.value =
                                            (value == 'On');
                                        widget.strBloc.markDirty();
                                      }
                                      setState(() {
                                        featureOn = value;
                                      });
                                    }
                                  : null,
                              disabledHint: Text(
                                  widget.featureValue.locked
                                      ? 'Locked'
                                      : 'No access',
                                  style: Theme.of(context).textTheme.caption),
                            ),
                          ),
                        ),
                        Expanded(
                          flex: 2,
                          child: widget.rolloutStrategy != null
                              ? Container(
                                  height: 32,
                                  width: 32,
                                  child: Material(
                                    shape: CircleBorder(),
                                    child: IconButton(
                                      mouseCursor: !snap.data && canWrite
                                          ? SystemMouseCursors.click
                                          : null,
                                      icon: Icon(AntDesign.delete, size: 14),
                                      onPressed: !snap.data && canWrite
                                          ? () => widget.strBloc.removeStrategy(
                                              widget.rolloutStrategy)
                                          : null,
                                    ),
                                  ),
                                )
                              : SizedBox.shrink(),
                        )
                      ]),
                ),
              ),
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

class FeatureValueBooleanCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueBooleanCellEditor(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
        creator: (_c, _b) =>
            CustomStrategyBloc(environmentFeatureValue, feature, fvBloc),
        child: Builder(builder: (ctx) {
          final strategyBloc = BlocProvider.of<CustomStrategyBloc>(ctx);
          return StreamBuilder<List<RolloutStrategy>>(
              stream: strategyBloc.strategies,
              builder: (streamCtx, snap) {
                return Column(
                  mainAxisSize: MainAxisSize.max,
                  children: [
                    Expanded(
                      flex: 1,
                      child: FeatureValueEditLockedCell(
                        environmentFeatureValue: environmentFeatureValue,
                        feature: feature,
                        fvBloc: fvBloc,
                      ),
                    ),
                    Expanded(
                      flex: 1,
                      child: FeatureValueBooleanEnvironmentCell(
                        environmentFeatureValue: environmentFeatureValue,
                        feature: feature,
                        fvBloc: fvBloc,
                      ),
                    ),
                    if (snap.hasData)
                      for (RolloutStrategy strategy in snap.data)
                        Expanded(
                          flex: 1,
                          child: FeatureValueBooleanEnvironmentCell(
                            environmentFeatureValue: environmentFeatureValue,
                            feature: feature,
                            fvBloc: fvBloc,
                            strBloc: strategyBloc,
                            rolloutStrategy: strategy,
                          ),
                        ),
                    Expanded(
                      flex: 1,
                      child: StreamBuilder<bool>(
                          stream: fvBloc.environmentIsLocked(
                              environmentFeatureValue.environmentId),
                          builder: (context, snapshot) {
                            if (snapshot.hasData) {
                              return _AddStrategyButton(
                                  bloc: strategyBloc,
                                  fvBloc: fvBloc,
                                  locked: snapshot.data);
                            } else {
                              return Container();
                            }
                          }),
                    ),
                    Expanded(
                      flex: 2,
                      child: FeatureValueUpdatedByCell(
                        environmentFeatureValue: environmentFeatureValue,
                        feature: feature,
                        fvBloc: fvBloc,
                      ),
                    ),
                  ],
                );
              });
        }));
  }
}

class _AddStrategyButton extends StatelessWidget {
  final CustomStrategyBloc bloc;
  final FeatureValuesBloc fvBloc;
  final bool locked;

  const _AddStrategyButton({Key key, this.bloc, this.fvBloc, this.locked})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      child: FlatButton.icon(
          height: 24,
          label: Text('Split rollout'),
          textColor: Colors.white,
          disabledColor: Colors.black12,
          color: Theme.of(context).buttonColor,
          disabledTextColor: Colors.black38,
          icon: Icon(AntDesign.fork, color: Colors.white, size: 16.0),
          onPressed: (locked != true)
              ? () => fvBloc.mrClient.addOverlay((BuildContext context) {
                    //return null;
                    return CreateValueStrategyWidget(
                      fvBloc: fvBloc,
                      bloc: bloc,
                    );
                  })
              : null
//        BlocProvider.of<_CustomStrategyBloc>(context)
//            .addStrategy(RolloutStrategy());
          ),
    );
  }
}
