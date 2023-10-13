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
  }) : super(key: key);

  final bool unlocked;
  final bool editable;
  final RolloutStrategy? rolloutStrategy;
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
    if (widget.rolloutStrategy == null) {
        boolFeatureValue =
            (widget.strBloc.featureValue.valueBoolean ?? false) ? 'On' : 'Off';
      } else {
        boolFeatureValue = widget.rolloutStrategy!.value ? 'On' : 'Off';
      }
  }

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
        disabledHint:
            Text(boolFeatureValue, style: Theme.of(context).textTheme.bodySmall),
      ),
    );
  }

  void _updateFeatureValue(bool replacementBoolean) {
    if (widget.rolloutStrategy == null) {
      widget.strBloc.updateFeatureValueDefault(replacementBoolean);
    } else {
      widget.rolloutStrategy!.value = replacementBoolean;
      // widget.strBloc.updateStrategy();
    }
  }
}
