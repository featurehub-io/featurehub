
import 'package:app_singleapp/widgets/features/table-expanded-view/custom_strategy_attributes_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'add_attribute_strategy_widget.dart';

class RolloutStrategiesWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<IndividualStrategyBloc>(context);

    return Column(children: [
      StreamBuilder<List<RolloutStrategyAttribute>>(
          stream: bloc.attributes,
          builder: (context, snapshot) {
            if(snapshot.data.isNotEmpty) {
              return Column(children: [
                for(var rolloutStrategyAttribute in snapshot.data )
                  AttributeStrategyWidget(
                      attribute: rolloutStrategyAttribute)
              ]);
            }
            else {
              return Container();
            }
          }
      ),
      Row(
        children: [
          TextButton(onPressed: () => bloc.createAttribute(), child: Text('Add Custom')), //ToDo: onPressed should call a state change
          for(var e in StrategyAttributeWellKnownNames.values)
            TextButton(onPressed: () => bloc.createAttribute(type: e), child: Text('Add ${e.name}'))
        ],
      ),
    ],)
  }
}
