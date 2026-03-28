import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/matchers.dart';
import 'package:open_admin_app/widgets/strategyeditor/multiselect_dropdown.dart';
import 'package:open_admin_app/widgets/strategyeditor/transform_strategy_conditions.dart';
import 'package:open_admin_app/widgets/strategyeditor/transform_strategy_type_field.dart';

import 'attribute_value_chip_widget.dart';
import 'string_caps_extension.dart';

class EditAttributeStrategyWidget extends StatefulWidget {
  final EditingRolloutStrategyAttribute attribute;
  final bool attributeIsFirst;
  final StrategyEditorBloc bloc;

  // ignore: prefer_const_constructors, prefer_const_constructors_in_immutables
  EditAttributeStrategyWidget({
    Key? key,
    required this.attribute,
    required this.attributeIsFirst,
    required this.bloc,
  }) : super(key: key);

  @override
  EditAttributeStrategyWidgetState createState() =>
      EditAttributeStrategyWidgetState();
}

class EditAttributeStrategyWidgetState
    extends State<EditAttributeStrategyWidget> {
  final TextEditingController _fieldName = TextEditingController();
  final TextEditingController _value = TextEditingController();

  RolloutStrategyAttributeConditional? _dropDownCustomAttributeMatchingCriteria;
  StrategyAttributeWellKnownNames? _wellKnown;
  RolloutStrategyFieldType? _attributeType;

  List<RolloutStrategyAttributeConditional> _matchers = [];

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
    if (widget.attribute.fieldName != null) {
      _fieldName.text = widget.attribute.fieldName!;
    }

    _attributeType = widget.attribute.type; // which could be null

    _wellKnown = StrategyAttributeWellKnownNamesExtension.fromJson(
        widget.attribute.fieldName);

    _value.text = '';

    _matchers = defineMatchers(_attributeType, _wellKnown);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _didChange();
  }

  Widget _nameField() {
    final l10n = AppLocalizations.of(context)!;
    if (_wellKnown != null) {
      final nameFieldMap = {
        StrategyAttributeWellKnownNames.country: l10n.wellKnownCountry,
        StrategyAttributeWellKnownNames.device: l10n.wellKnownDevice,
        StrategyAttributeWellKnownNames.platform: l10n.wellKnownPlatform,
        StrategyAttributeWellKnownNames.version: l10n.wellKnownVersion,
        StrategyAttributeWellKnownNames.userkey: l10n.wellKnownUserKey,
      };
      return Text(nameFieldMap[_wellKnown!]!,
          style: Theme.of(context).textTheme.titleSmall!.copyWith());
    } else {
      return TextFormField(
          controller: _fieldName,
          decoration: InputDecoration(
              labelText: l10n.customKey,
              helperText: l10n.customKeyExample,
              labelStyle: Theme.of(context).textTheme.bodyLarge!.copyWith(
                  fontSize: 12.0,
                  color: Theme.of(context).buttonTheme.colorScheme?.primary)),
          style: const TextStyle(fontSize: 14.0),
          autofocus: true,
          textInputAction: TextInputAction.next,
          onChanged: (v) => _updateAttributeFieldName(),
          onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
          validator: ((v) {
            if (v == null || v.isEmpty) {
              return l10n.ruleNameRequired;
            }
            return null;
          }));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
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
                      shape: const CircleBorder(),
                      child: IconButton(
                          tooltip: AppLocalizations.of(context)!.deleteRule,
                          icon: const Icon(
                            Icons.delete_forever_sharp,
                            color: Colors.red,
                            size: 20.0,
                          ),
                          onPressed: () =>
                              widget.bloc.deleteAttribute(widget.attribute))),
                ],
              ),
            )
          ],
        ),
      ),
    );
  }

  Row _buildCondition(BuildContext context) {
    return Row(children: [
      Expanded(
        flex: 2,
        child: Column(
          children: [
            if (_wellKnown == null) _customFieldType(),
            Padding(
              padding: const EdgeInsets.only(left: 8.0),
              child: Container(
                padding: const EdgeInsets.all(4.0),
                // margin: EdgeInsets.all(8.0),
                decoration: const BoxDecoration(
                  borderRadius: BorderRadius.all(Radius.circular(6.0)),
                ),
                height: 42,
                child: OutlinedButton(
                  onPressed: () => {},
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<RolloutStrategyAttributeConditional>(
                      icon: const Icon(
                        Icons.keyboard_arrow_down,
                        size: 18,
                      ),
                      isExpanded: true,
                      items: _matchers.map((RolloutStrategyAttributeConditional
                          dropDownStringItem) {
                        return DropdownMenuItem<
                                RolloutStrategyAttributeConditional>(
                            value: dropDownStringItem,
                            child: Text(
                                transformStrategyAttributeConditionalValueToString(
                                    dropDownStringItem, _wellKnown),
                                style: Theme.of(context).textTheme.bodyMedium));
                      }).toList(),
                      hint: Text(AppLocalizations.of(context)!.selectCondition,
                          style: Theme.of(context).textTheme.titleSmall),
                      onChanged: (RolloutStrategyAttributeConditional? value) {
                        var readOnly = false; //TODO parametrise this if needed
                        if (!readOnly && value != null) {
                          setState(() {
                            _dropDownCustomAttributeMatchingCriteria = value;
                            widget.attribute.conditional = value;
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
      const SizedBox(width: 16.0),
      if (_wellKnown == StrategyAttributeWellKnownNames.country)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
                values: widget.attribute.values,
                possibleValues: StrategyAttributeCountryName.values,
                enumToDisplayNameMapper: _countryNameMapper,
                enumToJsonMapper: (e) =>
                    (e as StrategyAttributeCountryName).toJson(),
                jsonToEnumMapper: (e) =>
                    StrategyAttributeCountryNameExtension.fromJson(e),
                hint: AppLocalizations.of(context)!.selectCountry))
      else if (_wellKnown == StrategyAttributeWellKnownNames.device)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
              values: widget.attribute.values,
              possibleValues: StrategyAttributeDeviceName.values,
              enumToDisplayNameMapper: _deviceNameMapper,
              hint: AppLocalizations.of(context)!.selectDevice,
              enumToJsonMapper: (e) =>
                  (e as StrategyAttributeDeviceName).toJson(),
              jsonToEnumMapper: (e) =>
                  StrategyAttributeDeviceNameExtension.fromJson(e),
            ))
      else if (_wellKnown == StrategyAttributeWellKnownNames.platform)
        Expanded(
            flex: 4,
            child: MultiSelectDropdown(
              values: widget.attribute.values,
              possibleValues: StrategyAttributePlatformName.values,
              enumToDisplayNameMapper: _platformNameMapper,
              hint: AppLocalizations.of(context)!.selectPlatform,
              enumToJsonMapper: (e) =>
                  (e as StrategyAttributePlatformName).toJson(),
              jsonToEnumMapper: (e) =>
                  StrategyAttributePlatformNameExtension.fromJson(e),
            ))
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

    widget.attribute.fieldName = _fieldName.text;
  }

  Widget _customFieldType() {
    return Container(
      padding: const EdgeInsets.all(4.0),
      margin: const EdgeInsets.all(8.0),
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(6.0)),
      ),
      height: 42,
      child: OutlinedButton(
        onPressed: () => {},
        child: DropdownButtonHideUnderline(
          child: DropdownButton<RolloutStrategyFieldType>(
            icon: const Icon(
              Icons.keyboard_arrow_down,
              size: 18,
            ),
            isExpanded: true,
            items: RolloutStrategyFieldType.values
                .map((RolloutStrategyFieldType dropDownStringItem) {
              return DropdownMenuItem<RolloutStrategyFieldType>(
                  value: dropDownStringItem,
                  child: Text(
                      transformRolloutStrategyTypeFieldToString(
                          dropDownStringItem),
                      style: Theme.of(context).textTheme.bodyMedium));
            }).toList(),
            hint: Text(AppLocalizations.of(context)!.selectValueType,
                style: Theme.of(context).textTheme.titleSmall),
            onChanged: (RolloutStrategyFieldType? value) {
              if (value != null) {
                setState(() {
                  _attributeType = value;
                  widget.attribute.type = value;
                  _matchers = defineMatchers(_attributeType, _wellKnown);
                  _dropDownCustomAttributeMatchingCriteria = null;
                });
              }
            },
            value: _attributeType,
          ),
        ),
      ),
    );
  }

  Widget _fieldValueEditorByFieldType() {
    final l10n = AppLocalizations.of(context)!;
    String labelText;
    String helperText;
    var inputFormatters = <TextInputFormatter>[];
    switch (_attributeType) {
      case RolloutStrategyFieldType.STRING:
        switch (_wellKnown) {
          case StrategyAttributeWellKnownNames.userkey:
            labelText = l10n.userKeys;
            helperText = l10n.userKeyExample;
            break;
          case StrategyAttributeWellKnownNames.version:
            labelText = l10n.versions;
            helperText = l10n.versionExample;
            break;
          default:
            labelText = l10n.customValues;
            helperText = l10n.customValuesExample;
            break;
        }
        break;
      case RolloutStrategyFieldType.SEMANTIC_VERSION:
        labelText = l10n.versions;
        helperText = l10n.versionExample;
        break;
      case RolloutStrategyFieldType.NUMBER:
        labelText = l10n.numbers;
        helperText = l10n.numberExample;
        break;
      case RolloutStrategyFieldType.DATE:
        labelText = l10n.dates;
        helperText = l10n.dateExample;
        break;
      case RolloutStrategyFieldType.DATETIME:
        labelText = l10n.dateTimes;
        helperText = l10n.dateTimeExample;
        break;
      case RolloutStrategyFieldType.BOOLEAN:
        return Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: Container(
              padding: const EdgeInsets.all(4.0),
              margin: const EdgeInsets.all(8.0),
              decoration: const BoxDecoration(
                borderRadius: BorderRadius.all(Radius.circular(6.0)),
              ),
              height: 42,
              child: OutlinedButton(
                onPressed: () => {},
                child: DropdownButtonHideUnderline(
                  child: DropdownButton(
                    isDense: true,
                    icon: const Padding(
                      padding: EdgeInsets.only(left: 16.0),
                      child: Icon(
                        Icons.keyboard_arrow_down,
                        size: 18,
                      ),
                    ),
                    isExpanded: true,
                    items: <String>['true', 'false']
                        .map<DropdownMenuItem<String>>((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(
                          value,
                          style: Theme.of(context).textTheme.bodyMedium,
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
                    hint: Text(l10n.selectValue,
                        style: Theme.of(context).textTheme.titleSmall),
                  ),
                ),
              ),
            ));
      case RolloutStrategyFieldType.IP_ADDRESS:
        labelText = l10n.ipAddresses;
        helperText = l10n.ipAddressExample;
        break;
      default:
        return Container(); // nothing until they have chosen one
    }

    return Container(
      padding: const EdgeInsets.all(4.0),
      margin: const EdgeInsets.all(8.0),
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(6.0)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                constraints: const BoxConstraints(maxWidth: 250),
                child: TextFormField(
                  controller: _value,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(
                      border: const OutlineInputBorder(),
                      labelText: labelText,
                      helperText: helperText,
                      labelStyle: Theme.of(context).textTheme.bodyLarge!),
                  // readOnly: !widget.widget.editable,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  inputFormatters: inputFormatters,
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: Tooltip(
                  message: l10n.addValue,
                  child: TextButton.icon(
                    onPressed: () => _valueFieldChanged(_value.text),
                    icon: const Icon(Icons.add_outlined, size: 16.0),
                    label: Text(l10n.add),
                  ),
                ),
              )
            ],
          ),
          Wrap(
            spacing: 4.0,
            children: [
              for (dynamic val in widget.attribute.values)
                AttributeValueChipWidget(
                  label: val.toString(),
                  value: val,
                  onSelected: (e) =>
                      setState(() => widget.attribute.values.remove(e)),
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
    } else {
      List<String> valuesList =
          val.split(",").map((name) => name.trim()).toList();
      if (_attributeType == RolloutStrategyFieldType.NUMBER) {
        for (var element in valuesList) {
          try {
            final num = double.parse(element);
            if (!widget.attribute.values.contains(num)) {
              setState(() {
                widget.attribute.values.add(num);
                _value.text = '';
              });
            }

            // ignore: empty_catches
          } catch (e) {}
        }
      } else {
        if (!widget.attribute.values.contains(val)) {
          setState(() {
            for (var element in valuesList) {
              widget.attribute.values.add(element);
            }
            _value.text = '';
          });
        }
      }
    }
  }
}

String _countryNameMapper(dynamic val) {
  return ((val is StrategyAttributeCountryName)
          ? val.toJson().toString()
          : val.toString())
      .toString()
      .replaceAll('_', ' ')
      .replaceAll('of the', '')
      .replaceAll('of', '')
      .trim()
      .capitalizeFirstofEach;
}

String _deviceNameMapper(dynamic val) {
  return (val is StrategyAttributeDeviceName)
      ? val.toJson().toString()
      : val.toString();
}

String _platformNameMapper(dynamic val) {
  return (val is StrategyAttributePlatformName)
      ? val.toJson().toString()
      : val.toString();
}
