import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_feature_value_widget.dart';

class EditBooleanValueDropDownWidget extends EditFeatureValueWidget {
  const EditBooleanValueDropDownWidget({
    super.key,
    required super.unlocked,
    required super.canEdit,
    super.rolloutStrategy,
    super.groupRolloutStrategy,
    super.applicationRolloutStrategy,
    super.portfolioRolloutStrategy,
    required super.strBloc,
  });

  @override
  EditBooleanValueDropDownWidgetState createState() =>
      EditBooleanValueDropDownWidgetState();
}

class EditBooleanValueDropDownWidgetState
    extends EditFeatureValueState<EditBooleanValueDropDownWidget> {
  String boolFeatureValue = 'Off';

  @override
  void initState() {
    super.initState();
    final v = resolveStrategyValue();
    final boolValue = v != null
        ? v as bool
        : (widget.strBloc.featureValue.valueBoolean ?? false);
    boolFeatureValue = boolValue ? 'On' : 'Off';
  }

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: SizedBox(
        width: 100,
        child: OutlinedButton(
          onPressed: () {},
          child: DropdownButton<String>(
            underline: const SizedBox.shrink(),
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
            onChanged: widget.canEdit && widget.unlocked
                ? (value) {
                    final replacementBoolean = (value == 'On');
                    updateValue(replacementBoolean);
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
    );
  }
}
