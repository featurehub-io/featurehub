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

class _TextDeleteWidget extends StatelessWidget {
  final String label;
  final dynamic value;
  final ValueChanged<dynamic> onSelected;

  const _TextDeleteWidget(
      {Key key, @required this.label, @required this.value, this.onSelected})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: InkWell(
        onTap: () => onSelected(value),
        child: Row(
          children: [Text(label), Icon(Icons.delete_forever_sharp)],
        ),
      ),
    );
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
    return Column(
      children: [
        buildDropDown(context),
        Wrap(
          children: [
            for (dynamic val in widget.values)
              _TextDeleteWidget(
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
        )
      ],
    );
  }

  InkWell buildDropDown(BuildContext context) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      child: Container(
        constraints: BoxConstraints(maxWidth: 250),
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
    );
  }
}
