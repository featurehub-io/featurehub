import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'feature_values_bloc.dart';

class CreateValueStrategyWidget extends StatefulWidget {
  final FeatureValuesBloc fvBloc;
  final CustomStrategyBloc bloc;
  final RolloutStrategy rolloutStrategy;

  const CreateValueStrategyWidget({
    Key key,
    @required this.fvBloc,
    this.rolloutStrategy,
    this.bloc,
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
      _strategyPercentage.text = (widget.rolloutStrategy.percentage / 100).toString();
//      _dropDownStrategyType = widget.feature.valueType;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    final isReadOnly =
        !widget.fvBloc.mrClient.userIsFeatureAdminOfCurrentApplication;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.rolloutStrategy == null
            ? 'Add new value to split your feature rollout'
            : (isReadOnly ? 'View rollout strategy' : 'Edit rollout strategy')),
        content: Container(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              TextFormField(
                  controller: _strategyName,
                  decoration: InputDecoration(
                      labelText: 'Split rollout strategy name',
                      helperText: 'E.g. 20% rollout'),
                  readOnly: isReadOnly,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Strategy name required';
                    }
                    return null;
                  })),
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
                    items: [
                      DropdownMenuItem(
                          value: 'percentage',
                          child: Text('Percentage rollout'))
                    ],
                    hint: Text('Select rollout condition',
                        style: Theme.of(context).textTheme.subtitle2),
                    onChanged: (value) {
                      if (!isReadOnly) {
                        setState(() {
                          _dropDownStrategyType = value;
                        });
                      }
                    },
                    value: _dropDownStrategyType,
                  ),
                ),
              ),
              TextFormField(
                  controller: _strategyPercentage,
                  decoration: InputDecoration(
                      labelText: 'Percentage value',
                      helperText: 'You can enter a value with up to 4 decimal points, e.g. 0.0005 %'),
                  readOnly: isReadOnly,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  inputFormatters: [DecimalTextInputFormatter(
                      decimalRange: 4, activatedNegativeValues: false)],
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
          if (!isReadOnly)
            FHFlatButton(
                title: isUpdate ? 'Update' : 'Add',
                onPressed: (() async {
                  if (_formKey.currentState.validate()) {
                    try {
                      if (isUpdate) {
                      widget.rolloutStrategy..name = _strategyName.text..percentage = (double.parse(_strategyPercentage.text) * 100).toInt();
                      widget.bloc.updateStrategy();
                        widget.fvBloc.mrClient.removeOverlay();
                      } else {
                        if (_dropDownStrategyType != null) {
                          widget.bloc.addStrategy(RolloutStrategy()
                            ..name = _strategyName.text
                            ..percentage = (double.parse(_strategyPercentage.text) * 100).toInt()
                            ..value = false);
                          widget.fvBloc.mrClient.removeOverlay();
                        } else {
                          setState(() {
                            isError = true;
                          });
                        }
                      }
                    } catch (e, s) {
                      if (e is ApiException && e.code == 409) {
                        widget.fvBloc.mrClient.customError(
                            messageTitle:
                                "Strategy with name '${_strategyName.text}' already exists");
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
