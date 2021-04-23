import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class LockUnlockSwitch extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const LockUnlockSwitch({Key? key, this.environmentFeatureValue, this.fvBloc})
      : super(key: key);

  @override
  _LockUnlockSwitchState createState() => _LockUnlockSwitchState();
}

class _LockUnlockSwitchState extends State<LockUnlockSwitch> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          if (!snap.hasData) {
            return Container(
              height: lockHeight,
            );
          }

          // must always return the same "shaped" data in a table cell
          final disabled = (!widget.environmentFeatureValue.roles
                      .contains(RoleType.UNLOCK) &&
                  snap.data == true) ||
              (snap.data == false &&
                  !widget.environmentFeatureValue.roles
                      .contains(RoleType.LOCK));

          final locked = snap.hasData ? snap.data : true;

          Function pressed;
          if (!disabled) {
            pressed = () => widget.fvBloc.dirtyLock(
                widget.environmentFeatureValue.environmentId, !locked);
          }

          return Container(
            height: lockHeight,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                _LockUnlockIconButton(
                  lock: locked,
                  onPressed: pressed,
                )
              ],
            ),
          );
        });
  }
}

class _LockUnlockIconButton extends StatelessWidget {
  const _LockUnlockIconButton({
    Key? key,
    this.lock,
    this.onPressed,
  }) : super(key: key);

  final bool lock;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 36,
          height: 36,
          child: IconButton(
              splashRadius: 20,
              mouseCursor: onPressed != null ? SystemMouseCursors.click : null,
              tooltip: onPressed != null
                  ? (lock
                      ? 'Unlock to edit feature value'
                      : 'Lock feature value')
                  : null,
              icon: Icon(lock ? Icons.lock_outline : Icons.lock_open,
                  size: 20, color: lock ? Colors.red : Colors.green),
              onPressed: onPressed),
        ),
        Text(
          lock ? 'Locked' : 'Unlocked',
          style: Theme.of(context).textTheme.caption,
        )
      ],
    );
  }
}
