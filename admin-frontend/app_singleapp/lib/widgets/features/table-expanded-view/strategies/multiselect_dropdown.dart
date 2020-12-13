import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/attribute_value_chip_widget.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

typedef String MultiSelectMapPossibleValueToName(dynamic value);

class MultiSelectDropdown extends StatefulWidget {
  final List<dynamic> values;
  final List<dynamic> possibleValues;
  final String hint;
  final MultiSelectMapPossibleValueToName mapper;

  MultiSelectDropdown(this.values, this.possibleValues, this.mapper, this.hint);

  @override
  State<StatefulWidget> createState() {
    return _MultiSelectDropdownState();
  }
}

class _MultiSelectDropdownState extends State<MultiSelectDropdown> {
  List<dynamic> selectableValues = [];

  @override
  void didChangeDependencies() {
    selectableValues = widget.possibleValues
        .where((e) => !widget.values.contains(e))
        .toList(growable: true);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(4.0),
      margin: EdgeInsets.all(8.0),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(6.0)),
        color: Theme.of(context).cardColor,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          buildDropDown(context),
          Wrap(
            spacing: 4.0,
            children: [
                for (dynamic val in widget.values)
                  AttributeValueChipWidget(
                    label: widget.mapper(val),
                    value: val,
                    onSelected: (e) {
                      setState(() {
                        selectableValues.add(e);
                        widget.values.remove(e);
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
    return Container(
      height: 32,
      child: OutlinedButton(
        onPressed: () => {},
        child: DropdownButtonHideUnderline(
          child: DropdownButton(
            icon: Padding(
              padding: EdgeInsets.only(left: 8.0),
              child: Icon(
                Icons.keyboard_arrow_down,
                size: 24,
              ),
            ),
            isExpanded: true,
            items: selectableValues
                .map((e) => DropdownMenuItem(
                    value: e,
                    child: Text(widget.mapper(e),
                        style: Theme.of(context).textTheme.bodyText2)))
                .toList(),
            hint: Text(widget.hint, style: Theme.of(context).textTheme.subtitle2),
            onChanged: (value) {
              var readOnly = false; //TODO parametrise this if needed
              if (!readOnly) {
                setState(() {
                  widget.values.add(value);
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
