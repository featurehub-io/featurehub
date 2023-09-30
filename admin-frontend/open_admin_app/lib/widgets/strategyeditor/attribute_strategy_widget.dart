import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_utils.dart';

import 'add_attribute_strategy_widget.dart';

class AttributeStrategyWidget extends StatelessWidget {
  final EditingRolloutStrategyAttribute attribute;
  final bool attributeIsFirst;

  const AttributeStrategyWidget(
      {Key? key, required this.attribute, required this.attributeIsFirst})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<StrategyEditorBloc>(context);

    return StreamBuilder<List<RolloutStrategyViolation>>(
        stream: bloc.violationStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return const SizedBox.shrink();
          }

          final violation =
              snapshot.data!.firstWhereOrNull((vio) => vio.id == attribute.id);

          try {
            return Column(children: [
              if (!attributeIsFirst)
                Container(
                    padding: const EdgeInsets.all(4.0),
                    margin: const EdgeInsets.all(8.0),
                    decoration: BoxDecoration(
                      borderRadius:
                          const BorderRadius.all(Radius.circular(6.0)),
                      color: Theme.of(context).colorScheme.secondaryContainer,
                    ),
                    child: Text('AND',
                        style: Theme.of(context).textTheme.labelSmall)),
              EditAttributeStrategyWidget(
                attribute: attribute,
                attributeIsFirst: attributeIsFirst,
                bloc: bloc,
                key: ValueKey(attribute.id),
              ),
              if (violation != null)
                Text(violation.violation.toDescription(),
                    style: Theme.of(context)
                        .textTheme
                        .bodyMedium!
                        .copyWith(color: Theme.of(context).colorScheme.error))
            ]);
          } catch (e) {
            return const SizedBox.shrink();
          }
        });
  }
}
