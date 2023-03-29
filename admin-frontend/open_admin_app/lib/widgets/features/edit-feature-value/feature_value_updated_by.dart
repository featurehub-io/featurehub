import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/custom_strategy_bloc.dart';
import 'package:timeago/timeago.dart' as timeago;


class FeatureValueUpdatedByCell extends StatelessWidget {
  final CustomStrategyBloc strBloc;

  const FeatureValueUpdatedByCell({Key? key, required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var updatedBy = '';
    var whenUpdated = '';
    var whoUpdated = '';

    if (strBloc.featureValue.whenUpdated != null &&
        strBloc.featureValue.whoUpdated != null) {
      updatedBy = 'Updated by: ';
      whenUpdated = timeago.format(strBloc.featureValue.whenUpdated!.toLocal());
      whoUpdated = strBloc.featureValue.whoUpdated!.name!;
    }

    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Row(
        children: [
          Text(updatedBy,
              style: Theme.of(context)
                  .textTheme
                  .bodySmall!
                  .copyWith(color: Theme.of(context).colorScheme.primary)),
          Text(whoUpdated, style: Theme.of(context).textTheme.bodyLarge),
          const SizedBox(width: 8.0),
          Text(whenUpdated, style: Theme.of(context).textTheme.bodySmall)
        ],
      ),
    );
  }
}
