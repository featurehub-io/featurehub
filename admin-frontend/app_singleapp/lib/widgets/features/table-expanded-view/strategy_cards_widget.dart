import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/delete_strategy_icon_button.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/edit_value_strategy_link_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class StrategyCardsListWidget extends StatelessWidget {
  final bool editable;
  final Widget editableHolderWidget;
  final RolloutStrategy rolloutStrategy;
  final PerFeatureStateTrackingBloc fvBloc;
  final CustomStrategyBloc strBloc;

  const StrategyCardsListWidget(
      {Key key,
      @required this.editable,
      @required this.editableHolderWidget,
      @required this.rolloutStrategy,
      @required this.fvBloc,
      @required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: Card(
        color: rolloutStrategy == null ? Color(0xffeee6ff) : Color(0xfff2fde4),
        child: Padding(
          padding: const EdgeInsets.only(left: 8.0, right: 2.0),
          child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Expanded(
                    flex: 5,
                    child: rolloutStrategy == null
                        ? Text('default',
                            style: Theme.of(context).textTheme.caption)
                        : EditValueStrategyLinkButton(
                            editable: editable,
                            rolloutStrategy: rolloutStrategy,
                            fvBloc: fvBloc,
                            strBloc: strBloc,
                          )),
                Expanded(flex: 3, child: editableHolderWidget),
                Expanded(
                  flex: 2,
                  child: rolloutStrategy != null
                      ? DeleteStrategyIconButton(
                          editable: editable, rolloutStrategy: rolloutStrategy, strBloc: strBloc,)
                      : SizedBox.shrink(),
                )
              ]),
        ),
      ),
    );
  }
}
