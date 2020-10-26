import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class PlatformAttributeStrategyDropdown extends StatefulWidget {
  final RolloutStrategyAttribute attribute;

  const PlatformAttributeStrategyDropdown({Key key, this.attribute}) : super(key: key);

  @override
  _PlatformAttributeStrategyDropdownState createState() => _PlatformAttributeStrategyDropdownState();
}

class _PlatformAttributeStrategyDropdownState extends State<PlatformAttributeStrategyDropdown> {
  StrategyAttributePlatformName _strategyAttributePlatformName;

  @override
  void initState() {
    _strategyAttributePlatformName == widget?.attribute?.value;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      child: DropdownButton(
        icon: Padding(
          padding: EdgeInsets.only(left: 8.0),
          child: Icon(
            Icons.keyboard_arrow_down,
            size: 24,
          ),
        ),
        isExpanded: false,
        items: StrategyAttributePlatformName.values
            .map((StrategyAttributePlatformName dropDownStringItem) {
          return DropdownMenuItem<StrategyAttributePlatformName>(
              value: dropDownStringItem,
              child: Text(
                  StrategyAttributePlatformNameTypeTransformer.toJson(dropDownStringItem),
                  style: Theme
                      .of(context)
                      .textTheme
                      .bodyText2));
        }).toList(),

        hint: Text('Select platform',
            style: Theme
                .of(context)
                .textTheme
                .subtitle2),
        onChanged: (value) {
          var readOnly = false; //TODO parametrise this if needed
          if (!readOnly) {
            setState(() {
              _strategyAttributePlatformName = value;
            });
          }
        },
        value: _strategyAttributePlatformName,
      ),
    );
  }
}


