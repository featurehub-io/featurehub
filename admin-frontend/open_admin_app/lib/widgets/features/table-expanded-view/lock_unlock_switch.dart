import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_blocv2.dart';

class LockUnlockSwitch extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBlocV2 fvBloc;

  const LockUnlockSwitch(
      {Key? key, required this.environmentFeatureValue, required this.fvBloc})
      : super(key: key);

  @override
  _LockUnlockSwitchState createState() => _LockUnlockSwitchState();
}

class _LockUnlockSwitchState extends State<LockUnlockSwitch> {
  bool _locked = false;

  @override
  void initState() {
    super.initState();
    _locked = widget.fvBloc.currentFeatureValue!.locked;
  }

  @override
  Widget build(BuildContext context) {
    final disabled =
        (!widget.environmentFeatureValue.roles.contains(RoleType.UNLOCK) &&
                _locked == true) ||
            _locked == false &&
                !widget.environmentFeatureValue.roles.contains(RoleType.LOCK);

    return SizedBox(
      height: lockHeight,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Row(
            children: [
              SizedBox(
                width: 36,
                height: 36,
                child: IconButton(
                  splashRadius: 20,
                  mouseCursor: disabled
                      ? SystemMouseCursors.basic
                      : SystemMouseCursors.click,
                  tooltip: disabled
                      ? null
                      : (_locked
                          ? 'Unlock to edit feature value'
                          : 'Lock feature value'),
                  icon: Icon(_locked ? Icons.lock_outline : Icons.lock_open,
                      size: 20, color: _locked ? Colors.orange : Colors.green),
                  onPressed: () {
                    setState(() {
                      _locked = !_locked;
                    });
                    widget.fvBloc.updateFeatureValueLockedStatus(_locked);
                  },
                ),
              ),
              Text(
                _locked ? 'Locked' : 'Unlocked',
                style: Theme.of(context).textTheme.caption,
              )
            ],
          )
        ],
      ),
    );
  }
}
