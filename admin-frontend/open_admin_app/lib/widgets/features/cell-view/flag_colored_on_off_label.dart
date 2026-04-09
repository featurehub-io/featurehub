import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

class FlagOnOffColoredIndicator extends StatelessWidget {
  final bool on;

  const FlagOnOffColoredIndicator({Key? key, this.on = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return on
        ? Text(l10n.featureOn,
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                    color: const Color(0xff11C8B5),
            ))
        : Text(l10n.featureOff,
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                    color: const Color(0xffF44C49),
            ));
  }
}
