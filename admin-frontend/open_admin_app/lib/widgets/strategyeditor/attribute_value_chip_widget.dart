import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_label_container.dart';

class AttributeValueChipWidget extends StatelessWidget {
  final String label;
  final dynamic value;
  final ValueChanged<dynamic> onSelected;

  const AttributeValueChipWidget(
      {Key? key,
      required this.label,
      required this.value,
      required this.onSelected})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Chip(
            onDeleted: () => onSelected(value),
            label: Text(
              label,
            )),
        const FHLabelContainer(text: 'OR')
      ],
    );
  }
}
