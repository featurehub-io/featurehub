import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:app_singleapp/widgets/features/split_rollout_button.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

import 'create-strategy-widget.dart';
import 'custom_strategy_bloc.dart';
import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

class FeatureValueStringEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  FeatureValueStringEnvironmentCell(
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
  _FeatureValueStringEnvironmentCellState createState() =>
      _FeatureValueStringEnvironmentCellState();
}

class _FeatureValueStringEnvironmentCellState
    extends State<FeatureValueStringEnvironmentCell> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy.value
        : widget.featureValue.valueString;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canEdit = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          final isLocked = snap.hasData && snap.data;
          final enabled = canEdit && !isLocked;
          final editable = snap.hasData && !snap.data && canEdit;

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
                              flex: 4,
                              child: widget.rolloutStrategy == null
                                  ? Text('default',
                                      style:
                                          Theme.of(context).textTheme.caption)
                                  : FHUnderlineButton(
                                      enabled: editable,
                                      title: widget.rolloutStrategy.name,
                                      onPressed: editable
                                          ? () => {
                                                widget.fvBloc.mrClient
                                                    .addOverlay(
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
                              flex: 4,
                              child: _ValueEditContainer(
                                  enabled: enabled,
                                  tec: tec,
                                  canEdit: canEdit,
                                  widget: widget)),
                          Expanded(
                            flex: 2,
                            child: widget.rolloutStrategy != null
                                ? Container(
                                    height: 32,
                                    width: 32,
                                    child: Material(
                                      shape: CircleBorder(),
                                      child: IconButton(
                                        mouseCursor: editable
                                            ? SystemMouseCursors.click
                                            : null,
                                        icon: Icon(AntDesign.delete, size: 14),
                                        onPressed: editable
                                            ? () => widget.strBloc
                                                .removeStrategy(
                                                    widget.rolloutStrategy)
                                            : null,
                                      ),
                                    ),
                                  )
                                : SizedBox.shrink(),
                          )
                        ]),
                  )));
        });
  }
}

class _ValueEditContainer extends StatelessWidget {
  const _ValueEditContainer({
    Key key,
    @required this.enabled,
    @required this.tec,
    @required this.canEdit,
    @required this.widget,
  }) : super(key: key);

  final bool enabled;
  final TextEditingController tec;
  final bool canEdit;
  final FeatureValueStringEnvironmentCell widget;

  @override
  Widget build(BuildContext context) {
    return Container(
        width: 123,
        height: 30,
        child: TextField(
          style: Theme.of(context).textTheme.bodyText1,
          enabled: enabled,
          controller: tec,
          decoration: InputDecoration(
              contentPadding: EdgeInsets.only(left: 4.0, right: 4.0),
              enabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Theme.of(context).buttonColor,
              )),
              disabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Colors.grey,
              )),
              hintText:
                  canEdit ? 'Enter string value' : 'No editing permissions',
              hintStyle: Theme.of(context).textTheme.caption),
          onChanged: (value) {
            final replacementValue = value.isEmpty ? null : tec.text?.trim();
            if (widget.rolloutStrategy != null) {
              widget.rolloutStrategy.value = replacementValue;
              widget.strBloc.markDirty();
            } else {
              widget.fvBloc.dirty(widget.environmentFeatureValue.environmentId,
                  (current) => current.value = replacementValue);
            }
          },
        ));
  }
}

class FeatureValueStringCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueStringCellEditor(
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
            builder: (context, snapshot) {
              return Column(
                mainAxisSize: MainAxisSize.max,
                children: [
                  FeatureValueEditLockedCell(
                    environmentFeatureValue: environmentFeatureValue,
                    feature: feature,
                    fvBloc: fvBloc,
                  ),
                  FeatureValueStringEnvironmentCell(
                    environmentFeatureValue: environmentFeatureValue,
                    feature: feature,
                    fvBloc: fvBloc,
                  ),
                  if (snapshot.hasData)
                    for (RolloutStrategy strategy in snapshot.data)
                      FeatureValueStringEnvironmentCell(
                        environmentFeatureValue: environmentFeatureValue,
                        feature: feature,
                        fvBloc: fvBloc,
                        strBloc: strategyBloc,
                        rolloutStrategy: strategy,
                      ),
                  StreamBuilder<bool>(
                      stream: fvBloc.environmentIsLocked(
                          environmentFeatureValue.environmentId),
                      builder: (context, snapshot) {
                        if (snapshot.hasData) {
                          return AddStrategyButton(
                              bloc: strategyBloc,
                              fvBloc: fvBloc,
                              locked: snapshot.data);
                        } else {
                          return Container();
                        }
                      }),
                  FeatureValueUpdatedByCell(
                    environmentFeatureValue: environmentFeatureValue,
                    feature: feature,
                    fvBloc: fvBloc,
                  ),
                ],
              );
            });
      }),
    );
  }
}
