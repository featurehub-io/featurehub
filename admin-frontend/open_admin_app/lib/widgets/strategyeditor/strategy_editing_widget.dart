import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_outline_button.dart';
import 'package:open_admin_app/widgets/common/input_fields_validators/input_field_number_formatter.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/rollout_strategies_widget.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_utils.dart';

class StrategyEditingWidget extends StatefulWidget {
  final bool editable;
  final StrategyEditorBloc bloc;
  final String? returnToRoute;

  const StrategyEditingWidget({
    Key? key,
    required this.editable,
    required this.bloc,
    this.returnToRoute,
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

  bool isTotalPercentageError = false;
  bool showPercentageField = false;
  String? errorText;

  @override
  void initState() {
    super.initState();

    _strategyName.text = widget.bloc.rolloutStrategy.name ?? '';

    if (widget.bloc.rolloutStrategy.percentage != null) {
      _strategyPercentage.text = widget.bloc.rolloutStrategy.percentageText;
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    isTotalPercentageError = false;
    errorText = null;
  }

  @override
  Widget build(BuildContext context) {
    final focusNode = FocusScope.of(context);
    final ScrollController controller = ScrollController();

    return ScrollConfiguration(
      behavior: CustomScrollBehavior(),
      child: SingleChildScrollView(
        controller: controller,
        child: SizedBox(
          width: 1000,
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: TextFormField(
                        controller: _strategyName,
                        decoration: const InputDecoration(
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
                ),
                const SizedBox(height: 16),
                const RolloutStrategiesWidget(),
                const SizedBox(height: 16.0),
                const FHPageDivider(),
                const SizedBox(height: 16.0),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8.0),
                    child: Column(children: [
                      if ((widget.bloc.rolloutStrategy.percentage != null) ||
                          showPercentageField)
                        Row(
                          children: [
                            Flexible(
                              child: TextFormField(
                                controller: _strategyPercentage,
                                decoration: const InputDecoration(
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
                                  shape: const CircleBorder(),
                                  child: IconButton(
                                      icon: const Icon(
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
                                          widget.bloc.rolloutStrategy
                                              .percentage = null;
                                          showPercentageField = false;
                                        });
                                      })),
                            )
                          ],
                        ),
                    ]),
                  ),
                ),
                const SizedBox(height: 8.0),
                Row(
                  children: [
                    Text('Add percentage rollout rule',
                        style: Theme.of(context).textTheme.bodySmall),
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
                      style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                          color: Theme.of(context).colorScheme.error)),
                _NaughtyDataEntryWidget(bloc: widget.bloc),
                const SizedBox(height: 8.0),
                Align(
                  alignment: Alignment.bottomRight,
                  child: OverflowBar(
                    children: [
                      FHFlatButtonTransparent(
                        title: 'Cancel',
                        keepCase: true,
                        onPressed: () {
                          widget.returnToRoute != null
                              ? ManagementRepositoryClientBloc.router
                                  .navigateTo(context, widget.returnToRoute!)
                              : Navigator.pop(context);
                        },
                      ),
                      if (widget.editable)
                        FHFlatButton(
                            title: widget.bloc.rolloutStrategy.saved
                                ? 'Update'
                                : 'Add',
                            onPressed: () => _validationAction()),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _validationAction() async {
    if (_formKey.currentState!.validate()) {
      await _processUpdate();
    }
  }

  /// this updates the existing strategy we were provided and checks it for validation.
  /// if it passes we pass up the chain to update the strategy and pop the context, closing
  /// this window.
  Future<void> _processUpdate() async {
    final updatedStrategy = widget.bloc.rolloutStrategy.copy(
        name: _strategyName.text, attributes: widget.bloc.currentAttributes)
      ..percentageFromText = _strategyPercentage.text;

    await checkForViolationsAndPop(updatedStrategy, () async {
      await widget.bloc.strategyEditorProvider.updateStrategy(updatedStrategy);
    });
  }

  Future<void> checkForViolationsAndPop(
      EditingRolloutStrategy updatedStrategy, AsyncCallback onSuccess) async {
    final localValidationCheck = updatedStrategy.violations();

    if (localValidationCheck.isNotEmpty) {
      widget.bloc.updateLocalViolations(localValidationCheck);
    } else {
      final validationCheck = await widget.bloc.strategyEditorProvider
          .validateStrategy(updatedStrategy);

      if (validationCheck != null) {
        if (isValidationOk(validationCheck)) {
          await onSuccess();
          widget.returnToRoute != null
              ? ManagementRepositoryClientBloc.router
                  .navigateTo(context, widget.returnToRoute!)
              : Navigator.pop(context);
        } else {
          layoutValidationFailures(validationCheck, updatedStrategy);
        }
      }
    }
  }

  bool isValidationOk(RolloutStrategyValidationResponse validationCheck) {
    return validationCheck.customStategyViolations.isEmpty &&
        validationCheck.sharedStrategyViolations.isEmpty &&
        validationCheck.violations.isEmpty;
  }

  void layoutValidationFailures(
      RolloutStrategyValidationResponse validationCheck,
      EditingRolloutStrategy strategy) {
    widget.bloc.updateStrategyViolations(validationCheck);

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
  final StrategyEditorBloc bloc;

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
            return const SizedBox.shrink();
          }

          final globalErrors = snapshot.data!
              .where((vio) => vio.id == null)
              .map((e) => Text(e.violation.toDescription()))
              .toList();

          return Column(children: globalErrors);
        });
  }
}
