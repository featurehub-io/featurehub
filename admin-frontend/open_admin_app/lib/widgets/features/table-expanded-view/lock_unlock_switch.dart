import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';

class LockUnlockSwitch extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const LockUnlockSwitch(
      {Key? key, required this.environmentFeatureValue, required this.fvBloc})
      : super(key: key);

  @override
  _LockUnlockSwitchState createState() => _LockUnlockSwitchState();
}

class _LockUnlockSwitchState extends State<LockUnlockSwitch> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId!),
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

          VoidCallback? pressed;
          if (!disabled) {
            pressed = () => widget.fvBloc.dirtyLock(
                widget.environmentFeatureValue.environmentId!, locked != true);
          }

          return SizedBox(
            height: lockHeight,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                _LockUnlockIconButton(lock: locked == true, onPressed: pressed)
              ],
            ),
          );
        });
  }
}

class _LockUnlockIconButton extends StatelessWidget {
  const _LockUnlockIconButton({
    Key? key,
    required this.lock,
    this.onPressed,
  }) : super(key: key);

  final bool lock;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 36,
          height: 36,
          child: IconButton(
              splashRadius: 20,
              mouseCursor: onPressed != null
                  ? SystemMouseCursors.click
                  : SystemMouseCursors.basic,
              tooltip: onPressed != null
                  ? (lock
                      ? 'Click the lock to make changes'
                      : 'Click the lock to prevent further changes')
                  : null,
              icon: Icon(lock ? Icons.lock_outline : Icons.lock_open,
                  size: 20, color: lock ? Colors.orange : Colors.green),
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
