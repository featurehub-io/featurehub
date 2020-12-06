import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/strategy_utils.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/multiselect_dropdown.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_conditions.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/transform_strategy_type_field.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';

import 'matchers.dart';
import 'string_caps_extension.dart';

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

    _attributeType = _attribute.type; // which could be null

    _wellKnown = StrategyAttributeWellKnownNamesTypeTransformer
        .fromJsonMap[_attribute.fieldName ?? ''];

    _value.text = '';

    if (_attribute.values == null) {
      _attribute.values = [];
    }

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
              labelText: 'Custom rule name',
              helperText: 'e.g. warehouseId',
              labelStyle: Theme.of(context).textTheme.bodyText1.copyWith(
                  fontSize: 12.0, color: Theme.of(context).buttonColor)),
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
    return StreamBuilder<List<RolloutStrategyViolation>>(
        stream: widget.bloc.violationStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return SizedBox.shrink();
          }

          final violation = snapshot.data.firstWhere(
              (vio) => vio.id == widget.attribute.id,
              orElse: () => null);

          try {
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
                      child: Text('AND',
                          style: Theme.of(context).textTheme.overline)),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 8.0),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.all(Radius.circular(6.0)),
                    color: Theme.of(context).selectedRowColor,
                  ),
                  child: Row(
                    children: [
                      Expanded(flex: 2, child: _nameField()),
                      Expanded(flex: 7, child: _buildCondition(context)),
                      Expanded(
                        flex: 1,
                        child: Row(
                          mainAxisSize: MainAxisSize.max,
                          children: [
                            Material(
                                type: MaterialType.transparency,
                                shape: CircleBorder(),
                                child: IconButton(
                                    icon: Icon(
                                      Icons.delete_sharp,
                                      size: 18.0,
                                    ),
                                    hoverColor:
                                        Theme.of(context).primaryColorLight,
                                    splashRadius: 20,
                                    onPressed: () => widget.bloc
                                        .deleteAttribute(_attribute))),
                          ],
                        ),
                      )
                    ],
                  ),
                ),
                if (violation != null)
                  Text(violation.violation.toDescription(),
                      style: Theme.of(context)
                          .textTheme
                          .bodyText2
                          .copyWith(color: Theme.of(context).errorColor))
              ],
            );
          } catch (e, s) {
            print(e);
            print(s);
            return SizedBox.shrink();
          }
        });
  }

  Row _buildCondition(BuildContext context) {
    return Row(children: [
      if (_wellKnown == null)
        Expanded(
          flex: 2,
          child: Padding(
            padding: const EdgeInsets.only(left: 16.0),
            child: _customFieldType(),
          ),
        ),
      Expanded(
        flex: 2,
        child: Padding(
          padding: const EdgeInsets.only(left: 16.0),
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
                isExpanded: false,
                items: _matchers.map(
                    (RolloutStrategyAttributeConditional dropDownStringItem) {
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
        Expanded(
            flex: 4,
            child: Padding(
              padding: const EdgeInsets.only(left: 16.0),
              child: _fieldValueEditorByFieldType(),
            ))
    ]);
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
        return DropdownButton(
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
              : widget.attribute.values[0],
          onChanged: (value) {
            setState(() {
              widget.attribute.values = [value];
            });
          },
          hint: Text('Select value',
              style: Theme.of(context).textTheme.subtitle2),
        );
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
            _attribute.values = [value];
          });
        },
        value: _attribute.values.isEmpty ? null : _attribute.values[0] == true,
      );
    }

    return Column(
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
                    labelStyle: Theme.of(context).textTheme.bodyText1.copyWith(
                        fontSize: 12.0, color: Theme.of(context).buttonColor)),
                // readOnly: !widget.widget.editable,
                autofocus: true,
                onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                inputFormatters: inputFormatters,
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(left: 8.0),
              child: FHUnderlineButton(
                  onPressed: () => _valueFieldChanged(_value.text), title: 'Add'),
            )
          ],
        ),
        Wrap(
          spacing: 4.0,
          children: [
            for (dynamic val in _attribute.values)
              _TextDeleteWidget(
                label: val.toString(),
                value: val,
                onSelected: (e) => setState(() => _attribute.values.remove(e)),
              )
          ],
        )
      ],
    );
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

class _TextDeleteWidget extends StatelessWidget {
  final String label;
  final dynamic value;
  final ValueChanged<dynamic> onSelected;

  const _TextDeleteWidget(
      {Key key, @required this.label, @required this.value, this.onSelected})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Chip(
      onDeleted: () => onSelected(value),
      label: Text(label, style: Theme.of(context).textTheme.bodyText1,)
    );
  }
}

final _countryNameMapper = (dynamic val) => ((val is String)
        ? val
        : StrategyAttributeCountryNameTypeTransformer.toJson(val))
    .toString()
    .replaceAll('_', ' ')
    .replaceAll('of the', '')
    .replaceAll('of', '')
    .trim()
    .capitalizeFirstofEach;

final _deviceNameMapper = (dynamic val) => (val is String)
    ? val
    : StrategyAttributeDeviceNameTypeTransformer.toJson(val).toString();

final _platformNameMapper = (dynamic val) => (val is String)
    ? val
    : StrategyAttributePlatformNameTypeTransformer.toJson(val).toString();
