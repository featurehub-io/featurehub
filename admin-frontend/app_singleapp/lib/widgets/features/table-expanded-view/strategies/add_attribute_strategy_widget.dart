import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/custom_strategy_attributes_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/country_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/device_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/platform_attribute_strategy_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_conditions.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_type_field.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';

import 'matchers.dart';

class AttributeStrategyWidget extends StatefulWidget {
  final RolloutStrategyAttribute attribute;
  final bool attributeIsFirst;
  final IndividualStrategyBloc bloc;

  const AttributeStrategyWidget({
    Key key,
    @required this.attribute,
    @required this.attributeIsFirst,
    @required this.bloc,
  }) : super(key: key);

  @override
  _AttributeStrategyWidgetState createState() =>
      _AttributeStrategyWidgetState();
}

class _AttributeStrategyWidgetState extends State<AttributeStrategyWidget> {
  final TextEditingController _fieldName = TextEditingController();
  final TextEditingController _value = TextEditingController();

  RolloutStrategyAttributeConditional _dropDownCustomAttributeMatchingCriteria;
  RolloutStrategyAttribute _attribute;
  StrategyAttributeWellKnownNames _wellKnown;
  RolloutStrategyFieldType _attributeType;

  List<RolloutStrategyAttributeConditional> _matchers;

  _AttributeStrategyWidgetState();

  @override
  void initState() {
    _dropDownCustomAttributeMatchingCriteria = widget.attribute.conditional;
    super.initState();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _attribute = widget.attribute;

    if (_attribute.fieldName != null) {
      _fieldName.text = _attribute.fieldName;
    }

    if (_attribute.value != null) {
      _value.text = _attribute.value.toString();
    }

    _attributeType = _attribute.type; // which could be null

    _wellKnown = StrategyAttributeWellKnownNamesTypeTransformer
        .fromJsonMap[_attribute.fieldName ?? ''];

    _matchers = defineMatchers(_attributeType, _wellKnown);
  }


  final Map<StrategyAttributeWellKnownNames, String> _nameFieldMap = {
    StrategyAttributeWellKnownNames.country: 'Country',
    StrategyAttributeWellKnownNames.device: 'Device',
    StrategyAttributeWellKnownNames.platform: 'Platform',
    StrategyAttributeWellKnownNames.version: 'Version',
    StrategyAttributeWellKnownNames.userkey: 'User',
  };

  Widget _nameField() {
    if (_wellKnown != null) {
      return Text(_nameFieldMap[_wellKnown]);
    } else {
      return TextFormField(
          controller: _fieldName,
          decoration: InputDecoration(
              labelText: 'Custom field name',
              helperText: 'e.g. warehouseId',
              labelStyle: Theme.of(context).textTheme.bodyText1.copyWith(
                  fontSize: 12.0, color: Theme.of(context).buttonColor)),
          autofocus: true,
          onChanged: (v) => _updateAttributeFieldName(),
          onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
          validator: ((v) {
            if (v.isEmpty) {
              return 'Field name required';
            }
            return null;
          }));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (!widget.attributeIsFirst)
          Container(
              padding: EdgeInsets.all(4.0),
              margin: EdgeInsets.all(8.0),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.all(Radius.circular(6.0)),
                color: Theme.of(context).primaryColorLight,
              ),
              child: Text('AND', style: Theme.of(context).textTheme.overline)),
        Container(
          padding: EdgeInsets.symmetric(horizontal: 8.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.all(Radius.circular(6.0)),
            color: Theme.of(context).selectedRowColor,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.center,
            // mainAxisSize: MainAxisSize.max,
            children: [
              Expanded(flex: 1, child: _nameField()),
              if (_wellKnown == null)
                Expanded(flex: 1, child: _customFieldType()),
              Expanded(
                flex: 1,
                child: InkWell(
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
                      items: _matchers.map(
                          (RolloutStrategyAttributeConditional
                              dropDownStringItem) {
                        return DropdownMenuItem<
                                RolloutStrategyAttributeConditional>(
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
                            _attribute.conditional = value;
                          });
                        }
                      },
                      value: _dropDownCustomAttributeMatchingCriteria,
                    ),
                  ),
                ),
              ),
              SizedBox(width: 16.0),
              if (_wellKnown == StrategyAttributeWellKnownNames.country)
                Expanded(
                    flex: 3,
                    child:
                        CountryAttributeStrategyDropdown(attribute: _attribute))
              else if (_wellKnown == StrategyAttributeWellKnownNames.device)
                Expanded(
                    flex: 3,
                    child:
                        DeviceAttributeStrategyDropdown(attribute: _attribute))
              else if (_wellKnown == StrategyAttributeWellKnownNames.platform)
                Expanded(
                    flex: 3,
                    child: PlatformAttributeStrategyDropdown(
                        attribute: _attribute))
              else
                Expanded(
                    flex: 3,
                    child: Padding(
                      padding: const EdgeInsets.only(left: 16.0),
                      child: _fieldValueEditorByFieldType(),
                    )),
              Material(
                type: MaterialType.transparency,
                shape: CircleBorder(),
                child: IconButton(icon: Icon(Icons.delete_sharp, size: 18.0,),
                    hoverColor: Theme.of(context).primaryColorLight,
                    splashRadius: 20,
                  onPressed: () => widget.bloc.deleteAttribute(_attribute))
              )
            ],
          ),
        ),
      ],
    );
  }

  void _updateAttributeFieldName() {
    final newWellKnown = StrategyAttributeWellKnownNamesTypeTransformer
        .fromJsonMap[_fieldName.text ?? ''];

    if (newWellKnown != _wellKnown) {
      setState(() {
        _wellKnown = newWellKnown;
      });
    }

    _attribute.fieldName = _fieldName.text;
  }

  Widget _customFieldType() {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      child: Container(
        constraints: BoxConstraints(maxWidth: 250),
        child: DropdownButton(
          icon: Padding(
            padding: EdgeInsets.only(left: 16.0),
            child: Icon(
              Icons.keyboard_arrow_down,
              size: 24,
            ),
          ),
          isExpanded: true,
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
            setState(() {
              _attributeType = value;
              _attribute.type = value;
              _matchers = defineMatchers(_attributeType, _wellKnown);
              _dropDownCustomAttributeMatchingCriteria = null;
            });
          },
          value: _attributeType,
        ),
      ),
    );
  }

  Widget _fieldValueEditorByFieldType() {
    String labelText;
    String helperText;
    var inputFormatters = <TextInputFormatter>[];
    switch (_attributeType) {
      case RolloutStrategyFieldType.STRING:
        switch (_wellKnown) {
          case StrategyAttributeWellKnownNames.userkey:
            labelText = 'User key(s)';
            helperText = 'e.g. bob@xyz.com';
            break;
          case StrategyAttributeWellKnownNames.version:
            labelText = 'Version(s)';
            helperText = 'e.g. 1.3.4, 7.8.1-SNAPSHOT';
            break;
          default:
            labelText = 'Custom value(s)';
            helperText = 'e.g. WarehouseA, WarehouseB';
            break;
        }
        break;
      case RolloutStrategyFieldType.SEMANTIC_VERSION:
        labelText = 'Version(s)';
        helperText = 'e.g. 1.3.4, 7.8.1-SNAPSHOT';
        break;
      case RolloutStrategyFieldType.NUMBER:
        labelText = 'Number(s)';
        helperText = 'e.g. 6, 7.87543';
        inputFormatters = [
          DecimalTextInputFormatter(
              decimalRange: 6, activatedNegativeValues: true)
        ];
        break;
      case RolloutStrategyFieldType.DATE:
        labelText = 'Date(s) - YYYY/MM/DD';
        helperText = '2017/04/16';
        break;
      case RolloutStrategyFieldType.DATETIME:
        labelText = 'Date/Time(s) - ISO8601 format';
        helperText = 'e.g. 2007-03-01T13:00:00Z';
        break;
      case RolloutStrategyFieldType.BOOLEAN:
        // TODO: this needs a drop down for a true/false
        return Text('needs true/false container');
      case RolloutStrategyFieldType.IP_ADDRESS:
        labelText = 'IP Address(es) with or without CIDR';
        helperText = 'e.g. 168.192.54.3 or 192.168.86.1/8 or 10.34.0.0/32';
        break;
      default:
        return Container(); // nothing until they have chosen one
    }

    if (_attributeType == RolloutStrategyFieldType.BOOLEAN) {
      return DropdownButton(
        icon: Padding(
          padding: EdgeInsets.only(left: 16.0),
          child: Icon(
            Icons.keyboard_arrow_down,
            size: 24,
          ),
        ),
        isExpanded: true,
        items: [
          DropdownMenuItem(value: true, child: Text('true/on')),
          DropdownMenuItem(value: false, child: Text('false/off')),
        ],
        hint: Text('Select value type',
            style: Theme.of(context).textTheme.subtitle2),
        onChanged: (value) {
          setState(() {
            _attribute.value = value;
          });
        },
        value: _attribute.value == true,
      );
    }

    return TextFormField(
      controller: _value,
      decoration: InputDecoration(
          labelText: labelText,
          helperText: helperText,
          labelStyle: Theme.of(context)
              .textTheme
              .bodyText1
              .copyWith(fontSize: 12.0, color: Theme.of(context).buttonColor)),
      // readOnly: !widget.widget.editable,
      autofocus: true,
      onChanged: (v) => _valueFieldChanged(v),
      onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
      inputFormatters: inputFormatters,
    );
  }

  void _valueFieldChanged(String v) {
    if (v.trim().isEmpty) {
      _attribute.value = null;
    } else if (_attributeType == RolloutStrategyFieldType.NUMBER) {
      try {
        _attribute.value = double.parse(v);
      } catch (e) {}
    } else {
      _attribute.value = v;
    }
  }
}
