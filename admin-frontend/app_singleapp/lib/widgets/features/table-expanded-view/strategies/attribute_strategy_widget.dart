import 'package:app_singleapp/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/strategy_utils.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'add_attribute_strategy_widget.dart';

class AttributeStrategyWidget extends StatelessWidget {
  final RolloutStrategyAttribute attribute;
  final bool attributeIsFirst;

  const AttributeStrategyWidget(
      {Key key, this.attribute, this.attributeIsFirst})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<IndividualStrategyBloc>(context);

    return StreamBuilder<List<RolloutStrategyViolation>>(
        stream: bloc.violationStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return SizedBox.shrink();
          }

          final violation = snapshot.data
              .firstWhere((vio) => vio.id == attribute.id, orElse: () => null);

          try {
            return Column(children: [
              if (!attributeIsFirst)
                Container(
                    padding: EdgeInsets.all(4.0),
                    margin: EdgeInsets.all(8.0),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.all(Radius.circular(6.0)),
                      color: Theme.of(context).primaryColorLight,
                    ),
                    child: Text('AND',
                        style: Theme.of(context).textTheme.overline)),
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
                        .bodyText2
                        .copyWith(color: Theme.of(context).errorColor))
            ]);
          } catch (e, s) {
            print(e);
            print(s);
            return SizedBox.shrink();
          }
        });
  }
}
