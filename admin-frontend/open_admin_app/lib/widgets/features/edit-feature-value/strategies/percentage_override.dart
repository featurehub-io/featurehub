

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/percentage_utils.dart';

class PercentageOverrideWidget extends StatefulWidget {
  final RolloutStrategyInstance strategyInstance;
  final int? originalPercentageOverride;
  final bool editable;
  final ValueChanged<int?>? onPercentageOverrideChanged;

  PercentageOverrideWidget({super.key, required this.strategyInstance, required this.editable, this.onPercentageOverrideChanged}):
        originalPercentageOverride = strategyInstance.percentageOverride;

  @override
  State createState() {
    return _PercentageOverrideWidgetState();
  }
}

class _PercentageOverrideWidgetState extends State<PercentageOverrideWidget> {
  final TextEditingController _strategyPercentage = TextEditingController();
  final debouncer = Debouncer(milliseconds: 300);

  @override
  void initState() {
    super.initState();

    _strategyPercentage.text = percentageText(widget.strategyInstance.percentageOverride);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    _strategyPercentage.text = percentageText(widget.strategyInstance.percentageOverride);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return TextFormField(
      controller: _strategyPercentage,
      readOnly: !widget.editable,
      autofocus: true,
      onFieldSubmitted: (_) {
        // do nothing, we don't want to move to the next field
        // as thats "delete" and it triggers it immediately which
        // deletes the percentage
      },
      inputFormatters: [
        DecimalTextInputFormatter(
            decimalRange: 4,
            activatedNegativeValues: false)
      ],
      onChanged: (v) {
        debouncer.run(() {
          widget.strategyInstance.percentageOverride = percentageFromText(_strategyPercentage.text);
          if (widget.onPercentageOverrideChanged != null && widget.strategyInstance.percentageOverride != widget.originalPercentageOverride) {
            widget.onPercentageOverrideChanged!(widget.strategyInstance.percentageOverride);
          }
        });
      },
    );
  }
}
