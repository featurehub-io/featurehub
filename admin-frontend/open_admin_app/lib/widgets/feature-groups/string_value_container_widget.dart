import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';

class EditFeatureGroupStringValueContainer extends StatefulWidget {
  const EditFeatureGroupStringValueContainer({
    Key? key,
    required this.editable,
    required this.bloc,
    required this.feature,
  }) : super(key: key);

  final bool editable;
  final FeatureGroupFeature feature;
  final FeatureGroupBloc bloc;

  @override
  _EditFeatureGroupStringValueContainerState createState() =>
      _EditFeatureGroupStringValueContainerState();
}

class _EditFeatureGroupStringValueContainerState
    extends State<EditFeatureGroupStringValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    tec.text = (widget.feature.value ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
        width: 200,
        height: 36,
        child: TextField(
          style: Theme.of(context).textTheme.bodyLarge,
          enabled: !widget.feature.locked&& widget.editable,
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
              hintText: widget.editable
                  ? !widget.feature.locked? 'Enter string value'
                      : 'Unlock to edit'
                  : 'No editing rights',
              hintStyle: Theme.of(context).textTheme.bodySmall),
          onChanged: (value) {
            final replacementValue = value.isEmpty ? null : tec.text.trim();
            widget.bloc.setFeatureValue(replacementValue, widget.feature);
          },
        ));
  }
}
