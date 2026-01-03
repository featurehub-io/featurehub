import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';

class EditFeatureGroupBooleanValueWidget extends StatefulWidget {
  const EditFeatureGroupBooleanValueWidget({
    Key? key,
    required this.editable,
    required this.bloc,
    required this.feature,
  }) : super(key: key);

  final bool editable;
  final FeatureGroupFeature feature;
  final FeatureGroupBloc bloc;

  @override
  _EditFeatureGroupBooleanValueWidgetState createState() =>
      _EditFeatureGroupBooleanValueWidgetState();
}

class _EditFeatureGroupBooleanValueWidgetState
    extends State<EditFeatureGroupBooleanValueWidget> {
  String boolFeatureValue = 'Off';

  @override
  void initState() {
    super.initState();
    boolFeatureValue = widget.feature.value != null
        ? (widget.feature.value ? 'On' : 'Off')
        : 'Off';
  }

  @override
  Widget build(BuildContext context) {
    return DropdownButton(
      underline: const SizedBox.shrink(),
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
      onChanged: widget.editable && !widget.feature.locked
          ? (value) {
              final replacementBoolean = (value == 'On');
              _updateFeatureValue(replacementBoolean, widget.feature);
              setState(() {
                boolFeatureValue = replacementBoolean ? 'On' : 'Off';
              });
            }
          : null,
      disabledHint:
          Text(boolFeatureValue, style: Theme.of(context).textTheme.bodySmall),
    );
  }

  void _updateFeatureValue(
      bool replacementBoolean, FeatureGroupFeature feature) {
    widget.bloc.setFeatureValue(replacementBoolean, feature);
  }
}
