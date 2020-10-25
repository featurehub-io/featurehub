import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/percentage_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class CreateValueStrategyWidget extends StatefulWidget {
  final CustomStrategyBloc bloc;
  final RolloutStrategy rolloutStrategy;
  final bool editable;

  const CreateValueStrategyWidget({
    Key key,
    this.rolloutStrategy,
    @required this.bloc,
    @required this.editable,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _CreateValueStrategyWidgetState();
  }
}

class _CreateValueStrategyWidgetState extends State<CreateValueStrategyWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _strategyName = TextEditingController();
  final TextEditingController _strategyPercentage = TextEditingController();

  bool isUpdate = false;
  bool isTotalPercentageError = false;

  List<RolloutStrategyAttribute> rolloutStrategyAttributeList = [];

  @override
  void initState() {
    super.initState();
    if (widget.rolloutStrategy != null) {
      _strategyName.text = widget.rolloutStrategy.name;
      _strategyPercentage.text = widget.rolloutStrategy.percentageText;
      rolloutStrategyAttributeList = widget.rolloutStrategy?.attributes;
      isUpdate = true;
//      _dropDownCustomAttributeMatchingCriteria = 'percentage';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.rolloutStrategy == null
            ? 'Add percentage rollout strategy'
            : (widget.editable
                ? 'Edit rollout strategy'
                : 'View rollout strategy')),
        content: Container(
          width: 800,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
//              TextFormField(
//                  controller: _strategyName,
//                  decoration: InputDecoration(
//                      labelText: 'Rollout strategy name',
//                      helperText: 'E.g. 20% rollout'),
//                  readOnly: isReadOnly,
//                  autofocus: true,
//                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
//                  validator: ((v) {
//                    if (v.isEmpty) {
//                      return 'Strategy name required';
//                    }
//                    return null;
//                  })),
//              Padding(
//                padding: const EdgeInsets.only(top: 14.0),
//                child: InkWell(
//                  mouseCursor: SystemMouseCursors.click,
//                  child: DropdownButton(
//                    icon: Padding(
//                      padding: EdgeInsets.only(left: 8.0),
//                      child: Icon(
//                        Icons.keyboard_arrow_down,
//                        size: 24,
//                      ),
//                    ),
//                    isExpanded: false,
//                    items: [
//                      DropdownMenuItem(
//                          value: 'percentage',
//                          child: Text('Percentage rollout'))
//                    ],
//                    hint: Text('Select rollout condition',
//                        style: Theme.of(context).textTheme.subtitle2),
//                    onChanged: (value) {
//                      if (!isReadOnly) {
//                        setState(() {
//                          _dropDownStrategyType = value;
//                        });
//                      }
//                    },
//                    value: _dropDownStrategyType,
//                  ),
//                ),
//              ),
              TextFormField(
                  controller: _strategyPercentage,
                  decoration: InputDecoration(
                      labelText: 'Percentage value',
                      helperText:
                          'You can enter a value with up to 4 decimal points, e.g. 0.0005 %'),
                  readOnly: !widget.editable,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  inputFormatters: [
                    DecimalTextInputFormatter(
                        decimalRange: 4, activatedNegativeValues: false)
                  ],
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Percentage value required';
                    }
                    return null;
                  })),
              if(rolloutStrategyAttributeList.isNotEmpty) Column (children: [for(var rolloutStrategyAttribute in rolloutStrategyAttributeList )
                AttributeStrategyWidget(attribute: rolloutStrategyAttribute)]),
              if (isTotalPercentageError)
                Text(
                    'Your percentage total across all rollout values cannot be over 100%. Please enter different value.',
                    style: Theme.of(context)
                        .textTheme
                        .bodyText2
                        .copyWith(color: Theme.of(context).errorColor))
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.bloc.fvBloc.mrClient.removeOverlay();
            },
          ),
          if (widget.editable)
            FHFlatButton(
                title: isUpdate ? 'Update' : 'Add',
                onPressed: () => _validationAction()),
        ],
      ),
    );
  }

  void _validationAction() async {
    if (_formKey.currentState.validate()) {
      if (isUpdate) {
        await _processUpdate();
      } else {
        await _processCreate();
      }
    }
  }

  void _processUpdate() async {
    // this deals with the idea we may not have ids yet for stuff
    widget.bloc.ensureStrategiesAreUnique();

    final update = widget.rolloutStrategy.copyWith()
      ..name = _strategyPercentage.text
      ..percentageFromText = _strategyPercentage.text;

    final validationCheck = await widget.bloc.validationCheck(update);

    if (isValidationOk(validationCheck)) {
      widget.rolloutStrategy
        ..name = _strategyPercentage.text
        ..percentageFromText = _strategyPercentage.text;
      widget.bloc.updateStrategy();
      widget.bloc.fvBloc.mrClient.removeOverlay();
    } else {
      layoutValidationFailures(validationCheck);
    }
  }

  bool isValidationOk(RolloutStrategyValidationResponse validationCheck) {
    return validationCheck.customStategyViolations.isEmpty &&
        validationCheck.sharedStrategyViolations.isEmpty &&
        validationCheck.violations.isEmpty;
  }

  void _processCreate() async {
    // this deals with the idea we may not have ids yet for stuff
    widget.bloc.ensureStrategiesAreUnique();

    final defaultValue =
        widget.bloc.feature.valueType == FeatureValueType.BOOLEAN
            ? false
            : null;

    final newStrategy = RolloutStrategy()
      ..name = _strategyPercentage.text
      ..percentageFromText = _strategyPercentage.text
      ..value = defaultValue;

    final validationCheck = await widget.bloc.validationCheck(newStrategy);

    if (isValidationOk(validationCheck)) {
      newStrategy.id = DateTime.now().millisecond.toString();
      widget.bloc.addStrategy(newStrategy);
      widget.bloc.fvBloc.mrClient.removeOverlay();
    } else {
      layoutValidationFailures(validationCheck);
    }
  }

  void layoutValidationFailures(
      RolloutStrategyValidationResponse validationCheck) {
    setState(() {
      if (validationCheck.violations.contains(
          RolloutStrategyCollectionViolationType
              .percentageAddsOver100Percent)) {
        isTotalPercentageError = true;
      }
    });
  }
}

class AttributeStrategyWidget extends StatefulWidget {
  final RolloutStrategyAttribute attribute;

  const AttributeStrategyWidget({
    Key key, this.attribute,

  }) :  super(key: key);


  @override
  _AttributeStrategyWidgetState createState() => _AttributeStrategyWidgetState();
}

class _AttributeStrategyWidgetState extends State<AttributeStrategyWidget> {
  final TextEditingController _customAttributeKey = TextEditingController();
  final TextEditingController _customAttributeValue = TextEditingController();

  RolloutStrategyAttributeConditional _dropDownCustomAttributeMatchingCriteria;
  bool isUpdate = false;

  _AttributeStrategyWidgetState();

  @override
  void initState() {
    super.initState();
    if (widget.attribute != null) {
      _dropDownCustomAttributeMatchingCriteria = widget.attribute.conditional;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.max,
      children: [
        Flexible(
          child: TextFormField(
              controller: _customAttributeKey,
              decoration: InputDecoration(
                  labelText: 'Custom attribute key',
                  helperText:
                  'E.g. userId'),
              // readOnly: !widget.widget.editable,
              // autofocus: true,
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
        Padding(
     padding: const EdgeInsets.only(top: 14.0),
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
                     _transformValuesToString(dropDownStringItem),
                     style: Theme.of(context).textTheme.bodyText2));
           }).toList(),

         hint: Text('Select condition',
             style: Theme.of(context).textTheme.subtitle2),
         onChanged: (value) {
           var readOnly = true;//TODO parametrise this
           if (!readOnly) {
             setState(() {
               _dropDownCustomAttributeMatchingCriteria = value;
             });
           }
         },
         value: _dropDownCustomAttributeMatchingCriteria,
       ),
     )),
        Flexible(
          child: TextFormField(
              controller: _customAttributeValue,
              decoration: InputDecoration(
                  labelText: 'Custom attribute value(s)',
                  helperText:
                  'E.g. bob@xyz.com, mary@xyz.com'),
              // readOnly: !widget.widget.editable,
              // autofocus: true,
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

  String _transformValuesToString(RolloutStrategyAttributeConditional dropDownStringItem) {
      switch (dropDownStringItem) {
        case RolloutStrategyAttributeConditional.EQUALS:
          return 'equals';
        case RolloutStrategyAttributeConditional.NOT_EQUALS:
          return 'not equals';
        case RolloutStrategyAttributeConditional.ENDS_WITH:
          return 'ends with';
        case RolloutStrategyAttributeConditional.STARTS_WITH:
          return 'starts with';
        case RolloutStrategyAttributeConditional.GREATER:
          return 'greater';
          break;
        case RolloutStrategyAttributeConditional.GREATER_EQUALS:
          return 'greater equals';
          break;
        case RolloutStrategyAttributeConditional.LESS:
          return 'less';
          break;
        case RolloutStrategyAttributeConditional.LESS_EQUALS:
          return 'less equals';
          break;
        case RolloutStrategyAttributeConditional.INCLUDES:
          return 'includes';
          break;
        case RolloutStrategyAttributeConditional.EXCLUDES:
          return 'excludes';
          break;
        case RolloutStrategyAttributeConditional.REGEX:
          return 'regex';
          break;
      }

      return '';
    }


}
