import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditBooleanValueDropDownWidget extends StatefulWidget {
  const EditBooleanValueDropDownWidget({
    Key key,
    @required this.editable,
    this.fvBloc,
    this.rolloutStrategy,
    this.strBloc,
    this.environmentFV,
    this.featureValue,
  }) : super(key: key);

  final bool editable;
  final PerFeatureStateTrackingBloc fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;
  final EnvironmentFeatureValues environmentFV;
  final FeatureValue featureValue;

  @override
  _EditBooleanValueDropDownWidgetState createState() =>
      _EditBooleanValueDropDownWidgetState();
}

class _EditBooleanValueDropDownWidgetState
    extends State<EditBooleanValueDropDownWidget> {
  String featureOn;
  @override
  Widget build(BuildContext context) {
    return DropdownButtonHideUnderline(
      child: DropdownButton(
        isDense: true,
        isExpanded: false,
        items:
            <String>['On', 'Off'].map<DropdownMenuItem<String>>((String value) {
          return DropdownMenuItem<String>(
            value: value,
            child: Text(
              value,
              style: Theme.of(context).textTheme.bodyText2,
            ),
          );
        }).toList(),
        value: featureOn,
        onChanged: widget.editable
            ? (value) {
                final replacementBoolean = (value == 'On');

                _notifyDirty(replacementBoolean);

                setState(() {
                  featureOn = value;
                });
              }
            : null,
        disabledHint: Text(featureOn,
            style: Theme.of(context).textTheme.caption),
      ),
    );
  }

  void _notifyDirty(bool replacementBoolean) {
    if (widget.rolloutStrategy == null) {
      widget.fvBloc.dirty(widget.environmentFV.environmentId,
          (current) => current.value = replacementBoolean);
    } else {
      widget.rolloutStrategy.value = replacementBoolean;
      widget.strBloc.markDirty();
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (widget.rolloutStrategy == null) {
      featureOn = (widget.featureValue.valueBoolean ?? false) ? 'On' : 'Off';
    } else {
      featureOn = widget.rolloutStrategy.value ? 'On' : 'Off';
    }
  }
}
