import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';

class RetireFeatureValueCheckboxWidget extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const RetireFeatureValueCheckboxWidget(
      {Key? key, required this.environmentFeatureValue, required this.fvBloc})
      : super(key: key);

  @override
  _RetireFeatureValueCheckboxWidgetState createState() =>
      _RetireFeatureValueCheckboxWidgetState();
}

class _RetireFeatureValueCheckboxWidgetState
    extends State<RetireFeatureValueCheckboxWidget> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc.isFeatureValueRetired(
            widget.environmentFeatureValue.environmentId!),
        builder: (ctx, snap) {
          if (!snap.hasData) {
            return const SizedBox.shrink();
          }

          final disabled = (!widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE));

          final retired = snap.hasData ? snap.data : true;

          VoidCallback? pressed;
          if (!disabled) {
            pressed = () => widget.fvBloc.dirtyLock(
                widget.environmentFeatureValue.environmentId!, retired != true);
          }

          return Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text("Retired", style: Theme.of(context).textTheme.caption),
              Checkbox(
                tristate: true,
                value: retired,
                onChanged: (bool? value) {
                  pressed;
                },
              ),
            ],
          );
        });
  }
}
