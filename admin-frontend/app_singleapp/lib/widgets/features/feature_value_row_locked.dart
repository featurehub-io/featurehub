import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class FeatureValueEditLockedCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;

  const FeatureValueEditLockedCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  _FeatureValueEditLockedCellState createState() =>
      _FeatureValueEditLockedCellState();
}

class _FeatureValueEditLockedCellState
    extends State<FeatureValueEditLockedCell> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
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
            height: 60.0,
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
    Key key,
    this.lock,
    this.onPressed,
  }) : super(key: key);

  final bool lock;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 36,
      height: 36,
      child: Material(
        shape: CircleBorder(),
        child: IconButton(
            icon: Icon(lock ? Icons.lock_outline : Icons.lock_open,
                size: 20, color: lock ? Colors.red : Colors.green),
            onPressed: onPressed),
      ),
    );
  }
}
