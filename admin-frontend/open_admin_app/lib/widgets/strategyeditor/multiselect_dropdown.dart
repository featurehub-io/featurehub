import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/strategyeditor/attribute_value_chip_widget.dart';

typedef MultiSelectMapEnumToDisplayName = String Function(dynamic value);
typedef MultiSelectMapEnumToJson = String Function(dynamic value);
typedef MultiSelectMapJsonToEnum = dynamic Function(String value);

class MultiSelectDropdown extends StatefulWidget {
  final List<dynamic> values; // these are the actual values that will be sent back to the server, they *must* be strings
  final List<dynamic> possibleValues;
  final String hint;
  final MultiSelectMapEnumToDisplayName enumToDisplayNameMapper;
  final MultiSelectMapEnumToJson enumToJsonMapper;
  final MultiSelectMapJsonToEnum jsonToEnumMapper;

  const MultiSelectDropdown(
      {required this.values, required this.possibleValues, required this.enumToDisplayNameMapper, required this.hint,
        required this.enumToJsonMapper, required this.jsonToEnumMapper,
        Key? key})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _MultiSelectDropdownState();
  }
}

class _MultiSelectDropdownState extends State<MultiSelectDropdown> {
  // this is the list of enum values that are selectable
  List<dynamic> selectableValues = [];
  List<dynamic> selectedValues = [];

  @override
  void didChangeDependencies() {
    // these are JSON values converted to ENUM values
    selectedValues = widget.values.map((e) => widget.jsonToEnumMapper(e)).whereNotNull().toList();
    // these are the ones in the list that aren't selected
    selectableValues = widget.possibleValues
        .where((e) => !selectedValues.contains(e))
        .toList();
    super.didChangeDependencies();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4.0),
      margin: const EdgeInsets.all(8.0),
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(6.0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          buildDropDown(context),
          Wrap(
            spacing: 4.0,
            children: [
              for (dynamic val in selectedValues)
                AttributeValueChipWidget(
                  label: widget.enumToDisplayNameMapper(val),
                  value: val, // this is an enum
                  onSelected: (e) {
                    setState(() {
                      // add in as a newly selectable value as we have removed it now
                      selectableValues.add(e);
                      // remove the enum value we are tracking
                      selectedValues.remove(e);
                      // remove the raw value
                      widget.values.remove(widget.enumToJsonMapper(e));
                    });
                  },
                )
            ],
          ),
        ],
      ),
    );
  }

  Widget buildDropDown(BuildContext context) {
    return SizedBox(
      height: 32,
      child: OutlinedButton(
        onPressed: () => {},
        child: DropdownButtonHideUnderline(
          child: DropdownButton<dynamic>(
            icon: const Padding(
              padding: EdgeInsets.only(left: 8.0),
              child: Icon(
                Icons.keyboard_arrow_down,
                size: 18,
              ),
            ),
            isExpanded: true,
            items: selectableValues
                .map((e) => DropdownMenuItem(
                value: e,
                child: Text(widget.enumToDisplayNameMapper(e),
                    style: Theme.of(context).textTheme.bodyMedium)))
                .toList(),
            hint:
            Text(widget.hint, style: Theme.of(context).textTheme.titleSmall),
            onChanged: (dynamic value) {
              var readOnly = false; //TODO parametrise this if needed
              if (!readOnly) {
                setState(() {
                  widget.values.add(widget.enumToJsonMapper(value));
                  selectedValues.add(value);
                  selectableValues.remove(value);
                });
              }
            },
            value: null,
          ),
        ),
      ),
    );
  }
}
