import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/multiselect_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_conditions.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_type_field.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';

import 'attribute_value_chip_widget.dart';
import 'matchers.dart';
import 'string_caps_extension.dart';

class EditAttributeStrategyWidget extends StatefulWidget {
  final RolloutStrategyAttribute attribute;
  final bool attributeIsFirst;
  final IndividualStrategyBloc bloc;

  const EditAttributeStrategyWidget({
    Key key,
    @required this.attribute,
    @required this.attributeIsFirst,
    @required this.bloc,
  }) : super(key: key);

  @override
  _EditAttributeStrategyWidgetState createState() =>
      _EditAttributeStrategyWidgetState();
}

class _EditAttributeStrategyWidgetState
    extends State<EditAttributeStrategyWidget> {
  final TextEditingController _fieldName = TextEditingController();
  final TextEditingController _value = TextEditingController();

  RolloutStrategyAttributeConditional _dropDownCustomAttributeMatchingCriteria;
  RolloutStrategyAttribute _attribute;
  StrategyAttributeWellKnownNames _wellKnown;
  RolloutStrategyFieldType _attributeType;

  List<RolloutStrategyAttributeConditional> _matchers;

  _EditAttributeStrategyWidgetState();

  @override
  void initState() {
    _dropDownCustomAttributeMatchingCriteria = widget.attribute.conditional;
    super.initState();
  }

  @override
  void didUpdateWidget(EditAttributeStrategyWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.attribute.id != widget.attribute.id) {
      _didChange();
    }
  }

  void _didChange() {
    _attribute = widget.attribute;

    if (_attribute.fieldName != null) {
      _fieldName.text = _attribute.fieldName;
    }

    _attributeType = _attribute.type; // which could be null

    _wellKnown =
        StrategyAttributeWellKnownNamesExtension.fromJson(_attribute.fieldName);

    _value.text = '';

    if (_attribute.values == null) {
      _attribute.values = [];
    } else {
      if (_wellKnown == StrategyAttributeWellKnownNames.platform) {
        _attribute.values =
            _attribute.values.map(_platformNameReverseMapper).toList();
      } else if (_wellKnown == StrategyAttributeWellKnownNames.device) {
        _attribute.values =
            _attribute.values.map(_deviceNameReverseMapper).toList();
      } else if (_wellKnown == StrategyAttributeWellKnownNames.country) {
        _attribute.values =
            _attribute.values.map(_countryNameReverseMapper).toList();
      }
    }

    _matchers = defineMatchers(_attributeType, _wellKnown);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _didChange();
  }

  final Map<StrategyAttributeWellKnownNames, String> _nameFieldMap = {
    StrategyAttributeWellKnownNames.country: 'Country',
    StrategyAttributeWellKnownNames.device: 'Device',
    StrategyAttributeWellKnownNames.platform: 'Platform',
    StrategyAttributeWellKnownNames.version: 'Version',
    StrategyAttributeWellKnownNames.userkey: 'User Key',
  };

  Widget _nameField() {
    if (_wellKnown != null) {
      return Text(
        _nameFieldMap[_wellKnown],
        style: Theme.of(context).textTheme.subtitle2.copyWith(
            color: Theme.of(context).brightness == Brightness.light
                ? Theme.of(context).buttonColor
                : Theme.of(context).accentColor),
      );
    } else {
      return TextFormField(
          controller: _fieldName,
          decoration: InputDecoration(
              labelText: 'Custom rule name',
              labelStyle: Theme.of(context).textTheme.bodyText1.copyWith(
                  fontSize: 12.0, color: Theme.of(context).buttonColor)),
          style: TextStyle(fontSize: 14.0),
          autofocus: true,
          onChanged: (v) => _updateAttributeFieldName(),
          onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
          validator: ((v) {
            if (v.isEmpty) {
              return 'Rule name required';
            }
            return null;
          }));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(8.0),
      margin: EdgeInsets.symmetric(vertical: 8.0),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(6.0)),
        color: Theme.of(context).brightness == Brightness.light
            ? Theme.of(context).selectedRowColor
            : Theme.of(context).primaryColorLight.withOpacity(0.1),
      ),
      child: Row(
        children: [
          Expanded(flex: 1, child: _nameField()),
          Expanded(flex: 7, child: _buildCondition(context)),
          Expanded(
            flex: 1,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Material(
                    type: MaterialType.transparency,
                    shape: CircleBorder(),
                    child: IconButton(
                        tooltip: 'Delete rule',
                        icon: Icon(
                          Icons.delete_forever_sharp,
                          color: Colors.red,
                          size: 20.0,
                        ),
                        hoverColor: Theme.of(context).primaryColorLight,
                        splashRadius: 20,
                        onPressed: () =>
                            widget.bloc.deleteAttribute(_attribute))),
              ],
            ),
          )
        ],
      ),
    );
  }

  Row _buildCondition(BuildContext context) {
    return Row(children: [
      Expanded(
        flex: 2,
        child: Column(
          children: [
            if (_wellKnown == null)
              Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: _customFieldType(),
              ),
            Padding(
              padding: const EdgeInsets.only(left: 8.0),
              child: Container(
                padding: EdgeInsets.all(4.0),
                margin: EdgeInsets.all(8.0),
                decoration: BoxDecoration(
                    borderRadius: BorderRadius.all(Radius.circular(6.0)),
                    color: Theme.of(context).cardColor),
                height: 32,
                child: OutlinedButton(
                  onPressed: () => {},
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton(
                      icon: Icon(
                        Icons.keyboard_arrow_down,
                        size: 24,
                      ),
                      isExpanded: true,
                      items: _matchers.map((RolloutStrategyAttributeConditional
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
            ),
          ],
        ),
      ),
      SizedBox(width: 16.0),
      if (_wellKnown == StrategyAttributeWellKnownNames.country)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
                _attribute.values,
                StrategyAttributeCountryName.values,
                _countryNameMapper,
                'Select Country'))
      else if (_wellKnown == StrategyAttributeWellKnownNames.device)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
                _attribute.values,
                StrategyAttributeDeviceName.values,
                _deviceNameMapper,
                'Select Device'))
      else if (_wellKnown == StrategyAttributeWellKnownNames.platform)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
                _attribute.values,
                StrategyAttributePlatformName.values,
                _platformNameMapper,
                'Select Platform'))
      else
        Expanded(flex: 4, child: _fieldValueEditorByFieldType())
    ]);
  }

  void _updateAttributeFieldName() {
    final newWellKnown =
        StrategyAttributeWellKnownNamesExtension.fromJson(_fieldName.text);

    if (newWellKnown != _wellKnown) {
      setState(() {
        _wellKnown = newWellKnown;
      });
    }

    _attribute.fieldName = _fieldName.text;
  }

  Widget _customFieldType() {
    return Container(
      padding: EdgeInsets.all(4.0),
      margin: EdgeInsets.all(8.0),
      decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
          color: Theme.of(context).cardColor),
      height: 32,
      child: OutlinedButton(
        onPressed: () => {},
        child: DropdownButtonHideUnderline(
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
        labelText = 'Date(s) - YYYY-MM-DD';
        helperText = '2017-04-16';
        break;
      case RolloutStrategyFieldType.DATETIME:
        labelText = 'Date/Time(s) - UTC/ISO8601 format';
        helperText = 'e.g. 2007-03-01T13:00:00Z';
        break;
      case RolloutStrategyFieldType.BOOLEAN:
        return Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: Container(
              padding: EdgeInsets.all(4.0),
              margin: EdgeInsets.all(8.0),
              decoration: BoxDecoration(
                  borderRadius: BorderRadius.all(Radius.circular(6.0)),
                  color: Theme.of(context).cardColor),
              height: 32,
              child: OutlinedButton(
                onPressed: () => {},
                child: DropdownButtonHideUnderline(
                  child: DropdownButton(
                    isDense: true,
                    icon: Padding(
                      padding: EdgeInsets.only(left: 16.0),
                      child: Icon(
                        Icons.keyboard_arrow_down,
                        size: 24,
                      ),
                    ),
                    isExpanded: true,
                    items: <String>['true', 'false']
                        .map<DropdownMenuItem<String>>((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(
                          value,
                          style: Theme.of(context).textTheme.bodyText2,
                        ),
                      );
                    }).toList(),
                    value: widget.attribute.values.isEmpty
                        ? null
                        : _asBoolean(widget.attribute.values[0]),
                    onChanged: (value) {
                      setState(() {
                        widget.attribute.values = [value];
                      });
                    },
                    hint: Text('Select value',
                        style: Theme.of(context).textTheme.subtitle2),
                  ),
                ),
              ),
            ));
      case RolloutStrategyFieldType.IP_ADDRESS:
        labelText = 'IP Address(es) with or without CIDR';
        helperText = 'e.g. 168.192.54.3 or 192.168.86.1/8 or 10.34.0.0/32';
        break;
      default:
        return Container(); // nothing until they have chosen one
    }

    return Container(
      padding: EdgeInsets.all(4.0),
      margin: EdgeInsets.all(8.0),
      decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
          color: Theme.of(context).cardColor),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                constraints: BoxConstraints(maxWidth: 250),
                child: TextFormField(
                  controller: _value,
                  decoration: InputDecoration(
                      border: OutlineInputBorder(),
                      labelText: labelText,
                      helperText: helperText,
                      labelStyle: Theme.of(context)
                          .textTheme
                          .bodyText1
                          .copyWith(
                              fontSize: 12.0,
                              color: Theme.of(context).buttonColor)),
                  // readOnly: !widget.widget.editable,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  inputFormatters: inputFormatters,
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: Tooltip(
                  message: 'Add value',
                  child: FHUnderlineButton(
                      onPressed: () => _valueFieldChanged(_value.text),
                      title: '+Add'),
                ),
              )
            ],
          ),
          Wrap(
            spacing: 4.0,
            children: [
              for (dynamic val in _attribute.values)
                AttributeValueChipWidget(
                  label: val.toString(),
                  value: val,
                  onSelected: (e) =>
                      setState(() => _attribute.values.remove(e)),
                )
            ],
          )
        ],
      ),
    );
  }

  String _asBoolean(dynamic val) {
    return (val is String)
        ? val.toLowerCase()
        : (val == true ? 'true' : 'false');
  }

  void _valueFieldChanged(String v) {
    final val = v.trim();
    if (val.isEmpty) {
      return;
    } else if (_attributeType == RolloutStrategyFieldType.NUMBER) {
      try {
        final num = double.parse(val);
        if (!_attribute.values.contains(num)) {
          setState(() {
            _attribute.values.add(num);
            _value.text = '';
          });
        }
        // ignore: empty_catches
      } catch (e) {}
    } else {
      if (!_attribute.values.contains(val)) {
        setState(() {
          _attribute.values.add(val);
          _value.text = '';
        });
      }
    }
  }
}

final _countryNameReverseMapper = (val) =>
    (val is String) ? StrategyAttributeCountryNameExtension.fromJson(val) : val;

final _countryNameMapper = (dynamic val) =>
    ((val is StrategyAttributeCountryName)
            ? val.toJson().toString()
            : val.toString())
        .toString()
        .replaceAll('_', ' ')
        .replaceAll('of the', '')
        .replaceAll('of', '')
        .trim()
        .capitalizeFirstofEach;

final _deviceNameMapper = (dynamic val) => (val is StrategyAttributeDeviceName)
    ? val.toJson().toString()
    : val.toString();

final _deviceNameReverseMapper = (val) =>
    (val is String) ? StrategyAttributeDeviceNameExtension.fromJson(val) : val;

final _platformNameReverseMapper = (val) => (val is String)
    ? StrategyAttributePlatformNameExtension.fromJson(val)
    : val;

final _platformNameMapper = (dynamic val) =>
    (val is StrategyAttributePlatformName)
        ? val.toJson().toString()
        : val.toString();
