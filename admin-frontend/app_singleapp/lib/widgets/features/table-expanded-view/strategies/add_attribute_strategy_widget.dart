import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
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
  RolloutStrategyFieldType _attributeType;

  _AttributeStrategyWidgetState();

  @override
  void initState() {
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
            onEditingComplete: () => _updateAttributeFieldName(),
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
    return Column(
      children: [
        Container(
            padding: EdgeInsets.all(4.0),
            margin: EdgeInsets.all(8.0),
            decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
          color: Theme.of(context).primaryColorLight,
        ), child: Text('AND', style: Theme.of(context).textTheme.overline)),
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
              if (_wellKnown == null) _customFieldType(),
              Expanded(
                flex: 1,
                child: InkWell(
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
                          _attribute.conditional = value;
                        });
                      }
                    },
                    value: _dropDownCustomAttributeMatchingCriteria,
                  ),
                ),
              ),
              SizedBox(width: 16.0),
              if (_wellKnown == StrategyAttributeWellKnownNames.country)
                Expanded(flex: 3, child: CountryAttributeStrategyDropdown(attribute: _attribute))
              else if (_wellKnown == StrategyAttributeWellKnownNames.device)
                Expanded(flex: 3, child: DeviceAttributeStrategyDropdown(attribute: _attribute))
              else if (_wellKnown == StrategyAttributeWellKnownNames.platform)
                Expanded(flex: 3, child: PlatformAttributeStrategyDropdown(attribute: _attribute))
              else
                Expanded(flex: 3, child: _fieldValueEditorByFieldType()),
            ],
          ),
        ),
      ],
    );
  }

  _updateAttributeFieldName() {
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
                  transformRolloutStrategyTypeFieldToString(dropDownStringItem),
                  style: Theme.of(context).textTheme.bodyText2));
        }).toList(),
        hint: Text('Select value type',
            style: Theme.of(context).textTheme.subtitle2),
        onChanged: (value) {
          setState(() {
            _attributeType = value;
          });
        },
        value: _attributeType,
      ),
    );
  }

  Widget _fieldValueEditorByFieldType() {
    String labelText;
    String helperText;
    List<TextInputFormatter> inputFormatters = [];
    switch (_attributeType) {
      case RolloutStrategyFieldType.STRING:
        switch (_wellKnown) {
          case StrategyAttributeWellKnownNames.userkey:
            labelText = 'User key(s)';
            helperText = 'e.g. bob@xyz.com';
            break;
          case StrategyAttributeWellKnownNames.session:
            labelText = 'Session id(s)';
            helperText = 'e.g. test uuids';
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
        helperText = 'e.g. 168.192.54.3 or 192.168.86.1/8';
        break;
      default:
        return Container(); // nothing until they have chosen one
    }

    return TextFormField(
        controller: _value,
        decoration:
            InputDecoration(labelText: labelText, helperText: helperText, labelStyle: Theme.of(context).textTheme.bodyText1.copyWith(fontSize: 12.0, color: Theme.of(context).primaryColor)),
        // readOnly: !widget.widget.editable,
        autofocus: true,
        // TODO: it actually has to be the right type, so a number has to be a number, a bool a bool
        onEditingComplete: () => _attribute.value = _value.text,
        onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
        inputFormatters: inputFormatters,
        validator: ((v) {
          if (v.isEmpty) {
            return 'Attribute value(s) required';
          }
          return null;
        }));
  }
}
