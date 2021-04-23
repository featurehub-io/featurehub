import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditBooleanValueDropDownWidget extends StatefulWidget {
  const EditBooleanValueDropDownWidget({
    Key? key,
    required this.editable,
    this.rolloutStrategy,
    this.strBloc,
  }) : super(key: key);

  final bool editable;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

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
        disabledHint:
            Text(featureOn, style: Theme.of(context).textTheme.caption),
      ),
    );
  }

  void _notifyDirty(bool replacementBoolean) {
    if (widget.rolloutStrategy == null) {
      widget.strBloc.fvBloc.dirty(
          widget.strBloc.environmentFeatureValue.environmentId,
          (current) => current.value = replacementBoolean);
    } else {
      widget.rolloutStrategy.value = replacementBoolean;
      widget.strBloc.markDirty();
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    String newOn;
    if (widget.rolloutStrategy == null) {
      newOn =
          (widget.strBloc.featureValue.valueBoolean ?? false) ? 'On' : 'Off';
    } else {
      newOn = widget.rolloutStrategy.value ? 'On' : 'Off';
    }

    if (newOn != featureOn) {
      setState(() {
        featureOn = newOn;
      });
    }
  }
}
