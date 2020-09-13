import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditNumberValueContainer extends StatefulWidget {
  const EditNumberValueContainer({
    Key key,
    @required this.enabled,
    @required this.canEdit,
    @required this.fvBloc,
    @required this.rolloutStrategy,
    @required this.strBloc,
    @required this.environmentFV,
    @required this.featureValue,
  }) : super(key: key);

  final bool enabled;
  final bool canEdit;
  final PerFeatureStateTrackingBloc fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;
  final EnvironmentFeatureValues environmentFV;
  final FeatureValue featureValue;


  @override
  _EditNumberValueContainerState createState() => _EditNumberValueContainerState();
}

class _EditNumberValueContainerState extends State<EditNumberValueContainer> {

  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy.value
        : widget.featureValue.valueNumber;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
        width: 123,
        height: 30,
        child: TextField(
          style: Theme.of(context).textTheme.bodyText1,
          enabled: widget.enabled,
          controller: tec,
          decoration: InputDecoration(
            contentPadding: EdgeInsets.only(left: 4.0, top: 4.0),
            enabledBorder: OutlineInputBorder(
                borderSide: BorderSide(
                  color: Theme.of(context).buttonColor,
                )),
            disabledBorder: OutlineInputBorder(
                borderSide: BorderSide(
                  color: Colors.grey,
                )),
            hintText: widget.canEdit
                ? 'Enter number value'
                : 'No editing permissions',
            hintStyle: Theme.of(context).textTheme.caption,
            errorText: validateNumber(tec.text) != null
                ? 'Not a valid number'
                : null,
          ),
          onChanged: (value) {
            final replacementValue = value.isEmpty ? null : tec.text?.trim();
            if (widget.rolloutStrategy != null) {
              widget.rolloutStrategy.value = double.parse(replacementValue);
              widget.strBloc.markDirty();
            } else {
              widget.fvBloc.dirty(widget.environmentFV.environmentId,
                      (current) => current.value = double.parse(replacementValue));
            }
          },
          inputFormatters: [
            DecimalTextInputFormatter(
                decimalRange: 5, activatedNegativeValues: true)
          ],
        ));
  }
}
