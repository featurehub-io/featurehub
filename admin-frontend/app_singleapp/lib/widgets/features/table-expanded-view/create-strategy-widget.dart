import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateValueStrategyWidget extends StatefulWidget {
  final PerFeatureStateTrackingBloc fvBloc;
  final CustomStrategyBloc bloc;
  final RolloutStrategy rolloutStrategy;
  final bool editable;

  const CreateValueStrategyWidget({
    Key key,
    @required this.fvBloc,
    @required this.rolloutStrategy,
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
  bool isError = false;
  String _dropDownStrategyType;

  @override
  void initState() {
    super.initState();
    if (widget.rolloutStrategy != null) {
      _strategyName.text = widget.rolloutStrategy.name;
      _strategyPercentage.text =
          (widget.rolloutStrategy.percentage / 100).toString();
//      _dropDownStrategyType = widget.feature.valueType;
      isUpdate = true;
//      _dropDownStrategyType = 'percentage';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.rolloutStrategy == null
            ? 'Add percentage rollout strategy'
            : (widget.editable ?  'Edit rollout strategy' : 'View rollout strategy')),
        content: Container(
          width: 500,
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
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.fvBloc.mrClient.removeOverlay();
            },
          ),
          if (widget.editable)
            FHFlatButton(
                title: isUpdate ? 'Update' : 'Add',
                onPressed: (() async {
                  if (_formKey.currentState.validate()) {
                    try {
                      if (isUpdate) {
                        widget.rolloutStrategy
                          ..name = _strategyPercentage.text
                          ..percentage =
                              (double.parse(_strategyPercentage.text) * 100)
                                  .toInt();
                        widget.bloc.updateStrategy();
                        widget.fvBloc.mrClient.removeOverlay();
                      } else {
                          var defaultValue;
                          //when creating new strategy - set value as "not set" (null) for strings,numbers, json. And set "false" for boolean
                          if(widget.bloc.featureValue.valueBoolean != null) {
                            defaultValue = false;
                          }
                          widget.bloc.addStrategy(RolloutStrategy()
                            ..name = _strategyPercentage.text
                            ..percentage =
                                (double.parse(_strategyPercentage.text) * 100)
                                    .toInt()
                            ..value = defaultValue);
                          widget.fvBloc.mrClient.removeOverlay();

                      }
                    } catch (e, s) {
                      if (e is ApiException && e.code == 409) {
                        widget.fvBloc.mrClient.customError(
                            messageTitle:
                                "Strategy with name '${_strategyPercentage.text}' already exists");
                      } else {
                        widget.fvBloc.mrClient.dialogError(e, s);
                      }
                    }
                  }
                }))
        ],
      ),
    );
  }
}
