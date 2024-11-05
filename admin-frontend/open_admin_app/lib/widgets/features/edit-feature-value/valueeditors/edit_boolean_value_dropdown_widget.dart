import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class EditBooleanValueDropDownWidget extends StatefulWidget {
  const EditBooleanValueDropDownWidget({
    Key? key,
    required this.unlocked,
    required this.editable,
    this.rolloutStrategy,
    required this.strBloc,
    this.groupRolloutStrategy,
    this.applicationRolloutStrategy,
  }) : super(key: key);

  final bool unlocked;
  final bool editable;
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  @override
  _EditBooleanValueDropDownWidgetState createState() =>
      _EditBooleanValueDropDownWidgetState();
}

class _EditBooleanValueDropDownWidgetState
    extends State<EditBooleanValueDropDownWidget> {
  String boolFeatureValue = 'Off';

  @override
  void initState() {
    super.initState();
    if (widget.rolloutStrategy != null) {
      boolFeatureValue = widget.rolloutStrategy!.value ? 'On' : 'Off';
    } else if (widget.groupRolloutStrategy != null) {
      boolFeatureValue = widget.groupRolloutStrategy!.value ? 'On' : 'Off';
    } else if (widget.applicationRolloutStrategy != null) {
      boolFeatureValue =
          widget.applicationRolloutStrategy!.value ? 'On' : 'Off';
    } else {
      boolFeatureValue =
          (widget.strBloc.featureValue.valueBoolean ?? false) ? 'On' : 'Off';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: SizedBox(
        width: 100,
        child: OutlinedButton(
          onPressed: () {},
          child: DropdownButtonHideUnderline(
            child: DropdownButton(
              isDense: true,
              isExpanded: true,
              items: <String>['On', 'Off']
                  .map<DropdownMenuItem<String>>((String value) {
                return DropdownMenuItem<String>(
                  value: value,
                  child: Text(
                    value,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                );
              }).toList(),
              value: boolFeatureValue,
              onChanged: widget.editable && widget.unlocked
                  ? (value) {
                      final replacementBoolean = (value == 'On');
                      _updateFeatureValue(replacementBoolean);
                      setState(() {
                        boolFeatureValue = replacementBoolean ? 'On' : 'Off';
                      });
                    }
                  : null,
              disabledHint: Text(boolFeatureValue,
                  style: Theme.of(context).textTheme.bodySmall),
            ),
          ),
        ),
      ),
    );
  }

  void _updateFeatureValue(bool replacementBoolean) {
    if (widget.rolloutStrategy != null) {
      widget.rolloutStrategy!.value = replacementBoolean;
      widget.strBloc.updateStrategyValue();
    } else if (widget.applicationRolloutStrategy != null) {
      widget.applicationRolloutStrategy!.value = replacementBoolean;
      widget.strBloc.updateApplicationStrategyValue();
    } else {
      widget.strBloc.updateFeatureValueDefault(replacementBoolean);
    }
  }
}
