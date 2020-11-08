import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class CountryAttributeStrategyDropdown extends StatefulWidget {
  final RolloutStrategyAttribute attribute;

  const CountryAttributeStrategyDropdown({Key key, this.attribute}) : super(key: key);

  @override
  _CountryAttributeStrategyDropdownState createState() => _CountryAttributeStrategyDropdownState();
}

class _CountryAttributeStrategyDropdownState extends State<CountryAttributeStrategyDropdown> {
  StrategyAttributeCountryName _strategyAttributeCountryName;

  @override
  void initState() {
    _strategyAttributeCountryName == widget?.attribute?.value;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
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
          items: StrategyAttributeCountryName.values
              .map((StrategyAttributeCountryName dropDownStringItem) {
            return DropdownMenuItem<StrategyAttributeCountryName>(
                value: dropDownStringItem,
                child: Text(
                    StrategyAttributeCountryNameTypeTransformer.toJson(dropDownStringItem),
                    style: Theme
                        .of(context)
                        .textTheme
                        .bodyText2));
          }).toList(),

          hint: Text('Select country',
              style: Theme
                  .of(context)
                  .textTheme
                  .subtitle2),
          onChanged: (value) {
            var readOnly = false; //TODO parametrise this if needed
            if (!readOnly) {
              setState(() {
                _strategyAttributeCountryName = value;
              });
            }
          },
          value: _strategyAttributeCountryName,
        ),
      ),
    );
  }
}


