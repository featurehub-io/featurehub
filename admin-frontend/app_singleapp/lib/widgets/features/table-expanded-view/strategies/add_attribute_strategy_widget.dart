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

  const AttributeStrategyWidget({
    Key key,
    this.attribute,
    this.bloc,
  }) : super(key: key);

  @override
  _AttributeStrategyWidgetState createState() =>
      _AttributeStrategyWidgetState();
}

class _AttributeStrategyWidgetState extends State<AttributeStrategyWidget> {
  final TextEditingController _customAttributeKey = TextEditingController();
  final TextEditingController _fieldName = TextEditingController();
  final TextEditingController _value = TextEditingController();

  RolloutStrategyAttributeConditional _dropDownCustomAttributeMatchingCriteria;
  RolloutStrategyAttribute _attribute;
  StrategyAttributeWellKnownNames _wellKnown;

  _AttributeStrategyWidgetState();

  @override
  void initState() {
    super.initState();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _attribute = widget.attribute.copyWith();

    if (_attribute.fieldName != null) {
      _fieldName.text = _attribute.fieldName;
    }

    if (_attribute.value != null) {
      _value.text = _attribute.value.toString();
    }

    _wellKnown = StrategyAttributeWellKnownNamesTypeTransformer
        .fromJsonMap[_attribute.fieldName ?? ''];
  }

  Map<StrategyAttributeWellKnownNames, String> _nameFieldMap = {
    StrategyAttributeWellKnownNames.country: 'Country',
    StrategyAttributeWellKnownNames.device: 'Device',
    StrategyAttributeWellKnownNames.platform: 'Platform',
    StrategyAttributeWellKnownNames.version: 'Version',
    StrategyAttributeWellKnownNames.userkey: 'User',
    StrategyAttributeWellKnownNames.session: 'Session',
  };

  Widget _nameField() {
    if (_wellKnown != null) {
      return Text(_nameFieldMap[_wellKnown]);
    } else {
      return Flexible(
        child: TextFormField(
            onEditingComplete: () => _updateAttribute(),
            controller: _fieldName,
            decoration: InputDecoration(
                labelText: 'Custom field name', helperText: 'e.g. warehouseId'),
            autofocus: true,
            onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
            validator: ((v) {
              if (v.isEmpty) {
                return 'Field name required';
              }
              return null;
            })),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.max,
      children: [
        _nameField(),
        Spacer(),
        if (attributeStrategyType == 'custom')
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
              items: RolloutStrategyFieldType.values
                  .map((RolloutStrategyFieldType dropDownStringItem) {
                return DropdownMenuItem<RolloutStrategyFieldType>(
                    value: dropDownStringItem,
                    child: Text(
                        transformRolloutStrategyTypeFieldToString(
                            dropDownStringItem),
                        style: Theme.of(context).textTheme.bodyText2));
              }).toList(),
              hint: Text('Select value type',
                  style: Theme.of(context).textTheme.subtitle2),
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
                      style: Theme.of(context).textTheme.bodyText2));
            }).toList(),
            hint: Text('Select condition',
                style: Theme.of(context).textTheme.subtitle2),
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
        if (attributeStrategyType == 'country')
          CountryAttributeStrategyDropdown(attribute: widget.attribute)
        else if (attributeStrategyType == 'device')
          DeviceAttributeStrategyDropdown()
        else if (attributeStrategyType == 'platform')
          PlatformAttributeStrategyDropdown()
        else
          Flexible(
            child: TextFormField(
                controller: _value,
                decoration: InputDecoration(
                    labelText: 'Custom attribute value(s)',
                    helperText: 'E.g. bob@xyz.com, mary@xyz.com'),
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
    final newWellKnown = StrategyAttributeWellKnownNamesTypeTransformer
        .fromJsonMap[_fieldName.text ?? ''];

    if (newWellKnown != _wellKnown) {
      setState(() {
        _wellKnown = newWellKnown;
      });
    }
    // widget.attribute
    //   ..value = _customAttributeKey
    //   ..value = _value;
    // widget.bloc.updateAttribute(attribute);
  }
}
