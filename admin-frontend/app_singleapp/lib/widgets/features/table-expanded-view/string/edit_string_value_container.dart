import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditStringValueContainer extends StatefulWidget {
  const EditStringValueContainer({
    Key? key,
    required this.enabled,
    required this.canEdit,
    this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool enabled;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBloc strBloc;

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
    return Container(
        width: 123,
        height: 30,
        child: TextField(
          style: Theme.of(context).textTheme.bodyText1,
          enabled: widget.enabled,
          controller: tec,
          decoration: InputDecoration(
              contentPadding:
                  EdgeInsets.only(left: 4.0, right: 4.0, bottom: 8.0),
              enabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Theme.of(context).buttonColor,
              )),
              disabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(
                color: Colors.grey,
              )),
              hintText: widget.canEdit
                  ? 'Enter string value'
                  : 'No editing permissions',
              hintStyle: Theme.of(context).textTheme.caption),
          onChanged: (value) {
            final replacementValue = value.isEmpty ? null : tec.text.trim();
            if (widget.rolloutStrategy != null) {
              widget.rolloutStrategy!.value = replacementValue;
              widget.strBloc.markDirty();
            } else {
              widget.strBloc.fvBloc.dirty(
                  widget.strBloc.environmentFeatureValue.environmentId!,
                  (current) => current.value = replacementValue);
            }
          },
        ));
  }
}
