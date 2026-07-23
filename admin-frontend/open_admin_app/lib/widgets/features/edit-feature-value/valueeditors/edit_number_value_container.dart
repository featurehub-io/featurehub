import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_feature_value_widget.dart';

class EditNumberValueContainer extends EditFeatureValueWidget {
  const EditNumberValueContainer({
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
  EditNumberValueContainerState createState() =>
      EditNumberValueContainerState();
}

class EditNumberValueContainerState
    extends EditFeatureValueState<EditNumberValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final v = resolveStrategyValue() ??
        widget.strBloc.currentFeatureValue.valueNumber;
    tec.text = (v ?? '').toString();
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
                    ? (widget.unlocked ? 'Enter number value' : 'Unlock to edit')
                    : 'No editing rights')
                : 'not set',
            hintStyle: Theme.of(context).textTheme.bodySmall,
            errorText:
                validateNumber(tec.text) != null ? 'Not a valid number' : null,
          ),
          onChanged: (value) {
            debouncer.run(() {
              updateValue(value.trim().isEmpty
                  ? null
                  : double.parse(tec.text.trim()));
            });
          },
          inputFormatters: [
            DecimalTextInputFormatter(
                decimalRange: 5, activatedNegativeValues: true)
          ],
        ));
  }
}
