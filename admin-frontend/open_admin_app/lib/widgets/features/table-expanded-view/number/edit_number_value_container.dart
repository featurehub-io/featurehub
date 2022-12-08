import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';

class EditNumberValueContainer extends StatefulWidget {
  const EditNumberValueContainer({
    Key? key,
    required this.unlocked,
    required this.canEdit,
    this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool unlocked;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;

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
        : widget.strBloc.featureValue.valueNumber;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
        width: 123,
        height: 30,
        child: TextField(
          style: Theme.of(context).textTheme.bodyText1,
          enabled: widget.canEdit && widget.unlocked,
          controller: tec,
          decoration: InputDecoration(
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
            hintText:
                widget.canEdit ?
                widget.unlocked ? 'Enter number value' : 'Unlock to edit' : 'No editing rights',
            hintStyle: Theme.of(context).textTheme.caption,
            errorText:
                validateNumber(tec.text) != null ? 'Not a valid number' : null,
          ),
          onChanged: (value) {
            final replacementValue =
                value.trim().isEmpty ? null : double.parse(tec.text.trim());
            _updateFeatureValue(replacementValue);

          },
          inputFormatters: [
            DecimalTextInputFormatter(
                decimalRange: 5, activatedNegativeValues: true)
          ],
        ));
  }
    void _updateFeatureValue(double? replacementValue) {
    if (widget.rolloutStrategy == null) {
    widget.strBloc.fvBloc.updateFeatureValueDefault(replacementValue);

    } else {
    widget.rolloutStrategy!.value = replacementValue;
    }
    }
}
