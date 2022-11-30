import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';

class EditStringValueContainer extends StatefulWidget {
  const EditStringValueContainer({
    Key? key,
    required this.unlocked,
    required this.canEdit,
    this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool unlocked;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;

  @override
  _EditStringValueContainerState createState() =>
      _EditStringValueContainerState();
}

class _EditStringValueContainerState extends State<EditStringValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy!.value
        : widget.strBloc.featureValue.valueString;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
        width: 123,
        height: 30,
        child: TextField(
          style: Theme.of(context).textTheme.bodyText1,
          enabled: widget.unlocked && widget.canEdit,
          controller: tec,
          decoration: InputDecoration(
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
              hintText:
                widget.canEdit ?
                widget.unlocked ? 'Enter string value' : 'Unlock to edit' : 'No editing rights',
              hintStyle: Theme.of(context).textTheme.caption),
          onChanged: (value) {
            final replacementValue = value.isEmpty ? null : tec.text.trim();
            if (widget.rolloutStrategy != null) {
              widget.rolloutStrategy!.value = replacementValue;
              // widget.strBloc.updateStrategy();
            } else {
              widget.strBloc.fvBloc.updateFeatureValueDefault(replacementValue);
            }
          },
        ));
  }
}
