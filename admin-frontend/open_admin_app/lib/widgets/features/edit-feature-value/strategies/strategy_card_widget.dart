import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_edit.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/delete_strategy_icon_button.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';

class StrategyCardWidget extends StatelessWidget {
  final bool editable;
  final Widget editableHolderWidget;
  final RolloutStrategy? rolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  const StrategyCardWidget(
      {Key? key,
      required this.editable,
      required this.editableHolderWidget,
      this.rolloutStrategy,
      required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var light = Theme.of(context).brightness == Brightness.light;
    return SizedBox(
      height: 50,
      child: InkWell(
        mouseCursor: rolloutStrategy == null ? null : SystemMouseCursors.grab,
        child: Card(
          color: rolloutStrategy == null
              ? (light ? defaultValueColor : defaultValueColorDark)
              : (light ? strategyValueColor : strategyValueColorDark),
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 2.0),
            child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Expanded(
                      flex: 3,
                      child: rolloutStrategy?.name == null
                          ? Text('default',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodySmall!
                                  .copyWith(color: defaultTextColor))
                          : Text(
                              rolloutStrategy!.name,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.labelLarge
                            )),
                  Flexible(flex: 6, child: editableHolderWidget),
                  Expanded(
                    flex: 3,
                    child: rolloutStrategy != null
                        ? Row(
                          children: [
                            EditValueStrategyLinkButton(
                              editable: editable,
                              rolloutStrategy: rolloutStrategy!,
                              fvBloc: strBloc,
                            ),
                            if (editable)
                            DeleteStrategyIconButton(
                              rolloutStrategy: rolloutStrategy!,
                              strBloc: strBloc,
                            ),
                          ],
                        )
                        : const SizedBox.shrink(),
                  )
                ]),
          ),
        ),
      ),
    );
  }
}
