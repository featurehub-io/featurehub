// ignore_for_file: library_private_types_in_public_api

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class ApplicationStrategiesDropDown extends StatefulWidget {
  final List<ListApplicationRolloutStrategyItem> strategies;
  final EditingFeatureValueBloc bloc;

  const ApplicationStrategiesDropDown(
      {Key? key, required this.strategies, required this.bloc})
      : super(key: key);

  @override
  _ApplicationStrategiesDropDownState createState() =>
      _ApplicationStrategiesDropDownState();
}

class _ApplicationStrategiesDropDownState
    extends State<ApplicationStrategiesDropDown> {
  String? currentValue;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 250,
      child: Padding(
        padding: const EdgeInsets.only(left: 8.0, right: 8.0),
        child: OutlinedButton(
            onPressed: () => {},
            child: DropdownButtonHideUnderline(
                child: DropdownButton(
              icon: const Padding(
                padding: EdgeInsets.only(left: 8.0),
                child: Icon(
                  Icons.keyboard_arrow_down,
                  size: 18,
                ),
              ),
              isExpanded: true,
              isDense: true,
              items: widget.strategies.isNotEmpty
                  ? widget.strategies
                      .map((ListApplicationRolloutStrategyItem strategy) {
                      return DropdownMenuItem<String>(
                          value: strategy.strategy.id,
                          child: Text(
                            strategy.strategy.name,
                            style: Theme.of(context).textTheme.bodyMedium,
                            overflow: TextOverflow.ellipsis,
                          ));
                    }).toList()
                  : null,
              hint: Text('Select strategy to add',
                  style: Theme.of(context).textTheme.titleSmall),
              onChanged: (String? value) async {
                setState(() {
                  currentValue = value;
                  widget.bloc.selectedStrategyToAdd = value;
                });
              },
              value: currentValue,
            ))),
      ),
    );
  }
}
