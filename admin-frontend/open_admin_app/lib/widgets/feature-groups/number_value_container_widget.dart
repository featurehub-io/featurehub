import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';

import 'feature_group_bloc.dart';

class EditFeatureGroupNumberValueContainer extends StatefulWidget {
  const EditFeatureGroupNumberValueContainer({
    Key? key,
    required this.editable,
    required this.feature,
    required this.bloc,
  }) : super(key: key);

  final FeatureGroupFeature feature;
  final bool editable;
  final FeatureGroupBloc bloc;

  @override
  _EditNumberValueContainerState createState() =>
      _EditNumberValueContainerState();
}

class _EditNumberValueContainerState
    extends State<EditFeatureGroupNumberValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    tec.text = (widget.feature.value ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
        width: 200,
        height: 36,
        child: TextField(
          style: Theme.of(context).textTheme.bodyLarge,
          enabled: !widget.feature.locked! && widget.editable,
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
            hintText: widget.editable
                ? !widget.feature.locked!
                    ? 'Enter number value'
                    : 'Unlock to edit'
                : 'No editing rights',
            hintStyle: Theme.of(context).textTheme.bodySmall,
            errorText:
                validateNumber(tec.text) != null ? 'Not a valid number' : null,
          ),
          onChanged: (value) {
            final replacementValue =
                value.isEmpty ? null : double.parse(tec.text.trim());
            widget.bloc.setFeatureValue(replacementValue, widget.feature);
          },
          inputFormatters: [
            DecimalTextInputFormatter(
                decimalRange: 5, activatedNegativeValues: true)
          ],
        ));
  }
}
