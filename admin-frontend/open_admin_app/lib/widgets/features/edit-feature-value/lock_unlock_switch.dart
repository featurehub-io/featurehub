import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class LockUnlockSwitch extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final EditingFeatureValueBloc fvBloc;

  const LockUnlockSwitch(
      {super.key, required this.environmentFeatureValue, required this.fvBloc});

  @override
  LockUnlockSwitchState createState() => LockUnlockSwitchState();
}

class LockUnlockSwitchState extends State<LockUnlockSwitch> {
  bool _locked = false;
  bool _initiallyLocked = false;

  @override
  void initState() {
    super.initState();
    _initiallyLocked = widget.fvBloc.currentFeatureValue.locked;
    _locked = _initiallyLocked;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final disabled =
        (!widget.environmentFeatureValue.roles.contains(RoleType.UNLOCK) &&
                _locked == true) ||
            _locked == false &&
                !widget.environmentFeatureValue.roles.contains(RoleType.LOCK);

    return Row(
      children: <Widget>[
        Row(
          children: [
            IconButton.outlined(
              tooltip: disabled ||
                      (_initiallyLocked &&
                          !_locked) // lock/unlock button will not be displayed if feature value was locked and is set to unlocked by user
                  // - this is to prevent user sending "locked" to the already "locked" feature)
                  ? null
                  : (_locked ? l10n.clickToUnlock : l10n.clickToLock),
              icon: Icon(_locked ? Icons.lock_outline : Icons.lock_open,
                  size: 20, color: _locked ? Colors.orange : Colors.green),
              onPressed: disabled || (_initiallyLocked && !_locked)
                  ? null
                  : () {
                      setState(() {
                        _locked = !_locked;
                      });
                      widget.fvBloc.updateFeatureValueLockedStatus(_locked);
                    },
            ),
            const SizedBox(
              width: 8.0,
            ),
            _locked
                ? Text(
                    l10n.featureIsLocked,
                    style: Theme.of(context)
                        .textTheme
                        .bodySmall
                        ?.copyWith(color: Colors.orange),
                  )
                : Text(
                    l10n.featureIsUnlocked,
                    style: Theme.of(context)
                        .textTheme
                        .bodySmall
                        ?.copyWith(color: Colors.green),
                  )
          ],
        )
      ],
    );
  }
}
