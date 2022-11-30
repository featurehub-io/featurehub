import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_blocv2.dart';

class RetireFeatureValueCheckboxWidget extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBlocV2 fvBloc;
  final bool editable;
  final bool retired;

  const RetireFeatureValueCheckboxWidget(
      {Key? key,
      required this.environmentFeatureValue,
      required this.fvBloc,
      required this.editable,
      required this.retired})
      : super(key: key);

  @override
  _RetireFeatureValueCheckboxWidgetState createState() =>
      _RetireFeatureValueCheckboxWidgetState();
}

class _RetireFeatureValueCheckboxWidgetState
    extends State<RetireFeatureValueCheckboxWidget> {
  bool retired = false;

  @override
  void initState() {
    super.initState();
    retired = widget.retired;
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Checkbox(
          value: retired,
          activeColor: Colors.red,
          onChanged: (bool? value) {
            if (widget.editable) {
              setState(() {
                retired = value!;
              });
              widget.fvBloc.updateFeatureValueRetiredStatus(value);
            } else {
              null;
            }
          },
        ),
        Text("Retired", style: Theme.of(context).textTheme.caption),
      ],
    );
  }
}
