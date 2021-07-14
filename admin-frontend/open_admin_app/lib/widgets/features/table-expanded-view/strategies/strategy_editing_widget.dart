import 'package:open_admin_app/widgets/common/FHFlatButton.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_outline_button.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/percentage_utils.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_utils.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

import 'rollout_strategies_widget.dart';

class StrategyEditingWidget extends StatefulWidget {
  final CustomStrategyBloc bloc;
  final bool editable;

  const StrategyEditingWidget({
    Key? key,
    required this.bloc,
    required this.editable,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _StrategyEditingWidgetState();
  }
}

class _StrategyEditingWidgetState extends State<StrategyEditingWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _strategyName = TextEditingController();
  final TextEditingController _strategyPercentage = TextEditingController();
  IndividualStrategyBloc? individualStrategyBloc;

  bool isUpdate = false;
  bool isTotalPercentageError = false;
  bool showPercentageField = false;
  String? errorText;

  @override
  void initState() {
    super.initState();

    individualStrategyBloc = BlocProvider.of(context);

    if (!individualStrategyBloc!.isUnsavedStrategy) {
      _strategyName.text = individualStrategyBloc!.rolloutStrategy.name;

      if (individualStrategyBloc!.rolloutStrategy.percentage != null) {
        _strategyPercentage.text =
            individualStrategyBloc!.rolloutStrategy.percentageText;
      }

      isUpdate = true;
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    individualStrategyBloc = BlocProvider.of(context);
    isTotalPercentageError = false;
    errorText = null;
  }

  @override
  Widget build(BuildContext context) {
    final focusNode = FocusScope.of(context);

    return FHAlertDialog(
      title: Text(
          widget.editable ? 'Edit split targeting' : 'View split targeting'),
      content: SingleChildScrollView(
        child: Container(
          width: 1000,
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 8.0),
                  decoration: BoxDecoration(
                      borderRadius: BorderRadius.all(Radius.circular(6.0)),
                      color:
                          Theme.of(context).primaryColorLight.withOpacity(0.3)),
                  child: TextFormField(
                      controller: _strategyName,
                      decoration: InputDecoration(
                          labelText: 'Split strategy name',
                          helperText: 'E.g. 20% rollout'),
                      readOnly: !widget.editable,
                      textInputAction: TextInputAction.next,
                      autofocus: true,
                      onFieldSubmitted: (_) => focusNode.nextFocus(),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Strategy name required';
                        }
                        return null;
                      })),
                ),
                SizedBox(height: 16),
                RolloutStrategiesWidget(),
                SizedBox(height: 16.0),
                FHPageDivider(),
                SizedBox(height: 16.0),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 8.0),
                  decoration: BoxDecoration(
                      borderRadius: BorderRadius.all(Radius.circular(6.0)),
                      color: Theme.of(context).brightness == Brightness.light
                          ? Theme.of(context).selectedRowColor
                          : Theme.of(context)
                              .primaryColorLight
                              .withOpacity(0.1)),
                  child: Column(children: [
                    if ((individualStrategyBloc!.rolloutStrategy.percentage !=
                            null) ||
                        showPercentageField)
                      Row(
                        children: [
                          Flexible(
                            child: TextFormField(
                              controller: _strategyPercentage,
                              decoration: InputDecoration(
                                  labelText: 'Percentage value',
                                  helperText:
                                      'You can enter a value with up to 4 decimal points, e.g. 0.0005 %'),
                              readOnly: !widget.editable,
                              autofocus: true,
                              onFieldSubmitted: (_) {
                                // do nothing, we don't want to move to the next field
                                // as thats "delete" and it triggers it immediately which
                                // deletes the percentage
                              },
                              inputFormatters: [
                                DecimalTextInputFormatter(
                                    decimalRange: 4,
                                    activatedNegativeValues: false)
                              ],
                              validator: ((v) {
                                if (v == null || v.isEmpty) {
                                  return 'Percentage value required';
                                }
                                return null;
                              }),
                            ),
                          ),
                          Flexible(
                            child: Material(
                                type: MaterialType.transparency,
                                shape: CircleBorder(),
                                child: IconButton(
                                    icon: Icon(
                                      Icons.delete_forever_sharp,
                                      color: Colors.red,
                                      size: 20.0,
                                    ),
                                    hoverColor:
                                        Theme.of(context).primaryColorLight,
                                    splashRadius: 20,
                                    onPressed: () {
                                      setState(() {
                                        _strategyPercentage.text = '';
                                        individualStrategyBloc!
                                            .rolloutStrategy.percentage = null;
                                        showPercentageField = false;
                                        widget.bloc.updateStrategy();
                                      });
                                    })),
                          )
                        ],
                      ),
                  ]),
                ),
                SizedBox(height: 8.0),
                Row(
                  children: [
                    Text('Add percentage rollout rule',
                        style: Theme.of(context).textTheme.caption),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 8.0),
                      child: FHOutlineButton(
                          onPressed: () {
                            setState(() {
                              showPercentageField = true;
                            });
                          },
                          title: '+ Percentage'),
                    ),
                  ],
                ),
                if (isTotalPercentageError)
                  Text(
                      'Your percentage total across all rollout values cannot be over 100%. Please enter different value.',
                      style: Theme.of(context)
                          .textTheme
                          .bodyText2!
                          .copyWith(color: Theme.of(context).errorColor)),
                _NaughtyDataEntryWidget(bloc: individualStrategyBloc!)
              ],
            ),
          ),
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
    );
  }

  void _validationAction() async {
    if (_formKey.currentState!.validate()) {
      if (isUpdate) {
        await _processUpdate();
      } else {
        await _processCreate();
      }
    }
  }

  Future<void> _processUpdate() async {
    // this deals with the idea we may not have ids yet for stuff
    widget.bloc.ensureStrategiesAreUnique();

    final updatedStrategy = individualStrategyBloc!.rolloutStrategy.copyWith()
      ..name = _strategyName.text
      ..attributes = individualStrategyBloc!.currentAttributes
      ..percentageFromText = _strategyPercentage.text;

    final validationCheck = await widget.bloc.validationCheck(updatedStrategy);

    if (isValidationOk(validationCheck)) {
      individualStrategyBloc!.rolloutStrategy
        ..name = _strategyName.text
        ..percentageFromText = _strategyPercentage.text;
      widget.bloc.updateStrategy();
      widget.bloc.fvBloc.mrClient.removeOverlay();
    } else {
      layoutValidationFailures(validationCheck, updatedStrategy);
    }
  }

  bool isValidationOk(RolloutStrategyValidationResponse validationCheck) {
    return validationCheck.customStategyViolations.isEmpty &&
        validationCheck.sharedStrategyViolations.isEmpty &&
        validationCheck.violations.isEmpty;
  }

  Future<void> _processCreate() async {
    // this deals with the idea we may not have ids yet for stuff
    widget.bloc.ensureStrategiesAreUnique();

    final defaultValue =
        widget.bloc.feature.valueType == FeatureValueType.BOOLEAN
            ? false
            : null;

    final newStrategy = RolloutStrategy(
      name: _strategyName.text,
      attributes: individualStrategyBloc!.currentAttributes,
      value: defaultValue,
    );

    if (_strategyPercentage.text.isNotEmpty) {
      newStrategy.percentageFromText = _strategyPercentage.text;
    }

    final validationCheck = await widget.bloc.validationCheck(newStrategy);

    if (isValidationOk(validationCheck)) {
      newStrategy.id = null;
      widget.bloc.addStrategy(newStrategy);
      widget.bloc.fvBloc.mrClient.removeOverlay();
    } else {
      layoutValidationFailures(validationCheck, newStrategy);
    }
  }

  void layoutValidationFailures(
      RolloutStrategyValidationResponse validationCheck,
      RolloutStrategy strategy) {
    individualStrategyBloc!.updateStrategyViolations(validationCheck, strategy);

    setState(() {
      if (validationCheck.violations.contains(
          RolloutStrategyCollectionViolationType
              .percentageAddsOver100Percent)) {
        isTotalPercentageError = true;
      }
    });
  }
}

class _NaughtyDataEntryWidget extends StatelessWidget {
  final IndividualStrategyBloc bloc;

  const _NaughtyDataEntryWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<List<RolloutStrategyViolation>>(
        stream: bloc.violationStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData ||
              snapshot.data!.isEmpty ||
              snapshot.data!.where((element) => element.id == null).isEmpty) {
            return SizedBox.shrink();
          }

          final globalErrors = snapshot.data!
              .where((vio) => vio.id == null)
              .map((e) => Text(e.violation.toDescription()))
              .toList();

          return Column(children: globalErrors);
        });
  }
}
