import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class EditNumberValueContainer extends StatefulWidget {
  const EditNumberValueContainer({
    Key? key,
    required this.unlocked,
    required this.canEdit,
    this.rolloutStrategy,
    required this.strBloc,
    this.groupRolloutStrategy,
    this.applicationRolloutStrategy,
  }) : super(key: key);

  final bool unlocked;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  @override
  _EditNumberValueContainerState createState() =>
      _EditNumberValueContainerState();
}

class _EditNumberValueContainerState extends State<EditNumberValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy!.value
        : widget.groupRolloutStrategy != null
            ? widget.groupRolloutStrategy!.value
            : widget.applicationRolloutStrategy != null
                ? widget.applicationRolloutStrategy!.value
                : widget.strBloc.currentFeatureValue.valueNumber;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    final debouncer = Debouncer(milliseconds: 1000);

    return SizedBox(
        height: 36,
        child: TextField(
          style: Theme.of(context).textTheme.bodyLarge,
          enabled: widget.canEdit && widget.unlocked,
          controller: tec,
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            contentPadding:
                const EdgeInsets.only(left: 4.0, top: 4.0, bottom: 8.0),
            enabledBorder: OutlineInputBorder(
                borderSide: BorderSide(
              color: Theme.of(context).colorScheme.primary,
            )),
            disabledBorder: const OutlineInputBorder(
                borderSide: BorderSide(
              color: Colors.grey,
            )),
            hintText: widget.groupRolloutStrategy == null
                ? (widget.canEdit
                    ? (widget.unlocked
                        ? 'Enter number value'
                        : 'Unlock to edit')
                    : 'No editing rights')
                : 'not set',
            hintStyle: Theme.of(context).textTheme.bodySmall,
            errorText:
                validateNumber(tec.text) != null ? 'Not a valid number' : null,
          ),
          onChanged: (value) {
            debouncer.run(
              () {
                final replacementValue =
                    value.trim().isEmpty ? null : double.parse(tec.text.trim());
                _updateFeatureValue(replacementValue);
              },
            );
          },
          inputFormatters: [
            DecimalTextInputFormatter(
                decimalRange: 5, activatedNegativeValues: true)
          ],
        ));
  }

  void _updateFeatureValue(double? replacementValue) {
    if (widget.rolloutStrategy != null) {
      widget.rolloutStrategy!.value = replacementValue;
      widget.strBloc.updateStrategyValue();
    } else if (widget.applicationRolloutStrategy != null) {
      widget.applicationRolloutStrategy!.value = replacementValue;
      widget.strBloc.updateApplicationStrategyValue();
    } else {
      widget.strBloc.updateFeatureValueDefault(replacementValue);
    }
  }
}
