import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/country_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/device_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/platform_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_conditions.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_type_field.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class AttributeStrategyWidget extends StatefulWidget {
  final RolloutStrategyAttribute attribute;
  final CustomStrategyBloc bloc;

  final  String attributeStrategyFieldName;

  const AttributeStrategyWidget({
    Key key, this.attribute, this.attributeStrategyFieldName, this.bloc,

  }) :  super(key: key);


  @override
  _AttributeStrategyWidgetState createState() => _AttributeStrategyWidgetState();
}

class _AttributeStrategyWidgetState extends State<AttributeStrategyWidget> {
  final TextEditingController _customAttributeKey = TextEditingController();
  final TextEditingController _customAttributeValue = TextEditingController();

  RolloutStrategyAttributeConditional _dropDownCustomAttributeMatchingCriteria;
  RolloutStrategyFieldType _rolloutStrategyFieldType;
  bool isUpdate = false;
  String attributeStrategyType;

  _AttributeStrategyWidgetState();

  @override
  void initState() {
    super.initState();
    if (widget.attribute != null) {
      _dropDownCustomAttributeMatchingCriteria = widget.attribute.conditional;
      isUpdate = true;
      attributeStrategyType = widget.attribute.fieldName;
    }
    else {
      attributeStrategyType = widget.attributeStrategyFieldName;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.max,
      children: [
        if(attributeStrategyType == 'country') Text('Country')
        else
          if (attributeStrategyType == 'device') Text('Device')
        else if(attributeStrategyType == 'platform') Text('Platform')
          else
            Flexible(
              child: TextFormField(
                  onEditingComplete: () => _updateAttribute(),
                  controller: _customAttributeKey,
                  decoration: InputDecoration(
                      labelText: 'Custom attribute key',
                      helperText:
                      'E.g. userId'),
                  // readOnly: !widget.widget.editable,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  // inputFormatters: [
                  //   DecimalTextInputFormatter(
                  //       decimalRange: 4, activatedNegativeValues: false)
                  // ],
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Attribute key required';
                    }
                    return null;
                  })),
            ),
        Spacer(),
        if(attributeStrategyType == 'custom') InkWell(
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
            items: RolloutStrategyFieldType.values
                .map((RolloutStrategyFieldType dropDownStringItem) {
              return DropdownMenuItem<RolloutStrategyFieldType>(
                  value: dropDownStringItem,
                  child: Text(
                      transformRolloutStrategyTypeFieldToString(
                          dropDownStringItem),
                      style: Theme
                          .of(context)
                          .textTheme
                          .bodyText2));
            }).toList(),

            hint: Text('Select value type',
                style: Theme
                    .of(context)
                    .textTheme
                    .subtitle2),
            onChanged: (value) {
              var readOnly = false; //TODO parametrise this if needed
              if (!readOnly) {
                setState(() {
                  _rolloutStrategyFieldType = value;
                });
              }
            },
            value: _rolloutStrategyFieldType,
          ),
        ),
        Spacer(),
        InkWell(
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
            items: RolloutStrategyAttributeConditional.values
                .map((RolloutStrategyAttributeConditional dropDownStringItem) {
              return DropdownMenuItem<RolloutStrategyAttributeConditional>(
                  value: dropDownStringItem,
                  child: Text(
                      transformStrategyAttributeConditionalValueToString(
                          dropDownStringItem),
                      style: Theme
                          .of(context)
                          .textTheme
                          .bodyText2));
            }).toList(),

            hint: Text('Select condition',
                style: Theme
                    .of(context)
                    .textTheme
                    .subtitle2),
            onChanged: (value) {
              var readOnly = false; //TODO parametrise this if needed
              if (!readOnly) {
                setState(() {
                  _dropDownCustomAttributeMatchingCriteria = value;
                });
              }
            },
            value: _dropDownCustomAttributeMatchingCriteria,
          ),
        ),
        Spacer(),
        if(attributeStrategyType == 'country') CountryAttributeStrategyDropdown(attribute: widget.attribute)
        else
          if (attributeStrategyType == 'device') DeviceAttributeStrategyDropdown()
          else if(attributeStrategyType == 'platform') PlatformAttributeStrategyDropdown()
          else
        Flexible(
          child: TextFormField(
              controller: _customAttributeValue,
              decoration: InputDecoration(
                  labelText: 'Custom attribute value(s)',
                  helperText:
                  'E.g. bob@xyz.com, mary@xyz.com'),
              // readOnly: !widget.widget.editable,
              autofocus: true,
              onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
              // inputFormatters: [
              //   DecimalTextInputFormatter(
              //       decimalRange: 4, activatedNegativeValues: false)
              // ],
              validator: ((v) {
                if (v.isEmpty) {
                  return 'Attribute value(s) required';
                }
                return null;
              })),
        ),
      ],
    );
  }

  _updateAttribute() {
    widget.attribute..value = _customAttributeKey..value = _customAttributeValue;
    widget.bloc.updateAttribute(attribute);
  }

}


