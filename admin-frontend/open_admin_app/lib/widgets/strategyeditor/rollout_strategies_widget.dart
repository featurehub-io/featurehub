import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_outline_button.dart';
import 'package:open_admin_app/widgets/strategyeditor/attribute_strategy_widget.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';

class RolloutStrategiesWidget extends StatelessWidget {
  const RolloutStrategiesWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<StrategyEditorBloc>(context);

    return Column(children: [
      StreamBuilder<List<EditingRolloutStrategyAttribute>>(
          stream: bloc.attributes,
          builder: (context, snapshot) {
            if (snapshot.hasData && snapshot.data!.isNotEmpty) {
              return Column(children: [
                for (var rolloutStrategyAttribute in snapshot.data!)
                  AttributeStrategyWidget(
                    attribute: rolloutStrategyAttribute,
                    attributeIsFirst:
                        rolloutStrategyAttribute == snapshot.data!.first,
                  )
              ]);
            } else {
              return Container();
            }
          }),
      const SizedBox(
        height: 16.0,
      ),
      StreamBuilder<List<EditingRolloutStrategyAttribute>>(
          stream: bloc.attributes,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return const SizedBox.shrink();
            }

            return Row(
              children: [
                Text('Add rule', style: Theme.of(context).textTheme.bodySmall),
                for (var e in StrategyAttributeWellKnownNames.values)
                  if (e != StrategyAttributeWellKnownNames.session &&
                      !snapshot.data!.any((rsa) =>
                          StrategyAttributeWellKnownNamesExtension.fromJson(
                              rsa.fieldName) ==
                          e))
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 8.0),
                      child: FHOutlineButton(
                          onPressed: () => bloc.createAttribute(type: e),
                          title: '+ ${e.name}'),
                    ),
              ],
            );
          }),
      const SizedBox(
        height: 8.0,
      ),
      Row(
        children: [
          Text('Add custom rule', style: Theme.of(context).textTheme.bodySmall),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            child: FHOutlineButton(
                onPressed: () => bloc.createAttribute(), title: '+ Custom'),
          ),
        ],
      ),
    ]);
  }
}
