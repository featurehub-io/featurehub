import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_feature_value_widget.dart';

class EditStringValueContainer extends EditFeatureValueWidget {
  const EditStringValueContainer({
    super.key,
    required super.unlocked,
    required super.canEdit,
    super.rolloutStrategy,
    super.groupRolloutStrategy,
    super.applicationRolloutStrategy,
    super.portfolioRolloutStrategy,
    required super.strBloc,
  });

  @override
  EditStringValueContainerState createState() =>
      EditStringValueContainerState();
}

class EditStringValueContainerState
    extends EditFeatureValueState<EditStringValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final v = resolveStrategyValue() ?? widget.strBloc.featureValue.valueString;
    tec.text = (v ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    final debouncer = Debouncer(milliseconds: 1000);

    return SizedBox(
        height: 36,
        child: TextField(
          style: Theme.of(context).textTheme.bodyLarge,
          enabled: widget.unlocked && widget.canEdit,
          controller: tec,
          decoration: InputDecoration(
              border: const OutlineInputBorder(),
              contentPadding:
                  const EdgeInsets.only(left: 4.0, right: 4.0, bottom: 8.0),
              enabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Theme.of(context).colorScheme.primary,
              )),
              disabledBorder: const OutlineInputBorder(
                  borderSide: BorderSide(
                color: Colors.grey,
              )),
              hintText: widget.groupRolloutStrategy == null
                  ? (widget.canEdit
                      ? (widget.unlocked ? 'Enter string value' : 'Unlock to edit')
                      : 'No editing rights')
                  : 'not set',
              hintStyle: Theme.of(context).textTheme.bodySmall),
          onChanged: (value) {
            debouncer.run(() {
              updateValue(value.isEmpty ? null : tec.text.trim());
            });
          },
        ));
  }
}
