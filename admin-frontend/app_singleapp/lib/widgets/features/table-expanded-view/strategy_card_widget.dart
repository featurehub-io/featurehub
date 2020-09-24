import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/delete_strategy_icon_button.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/edit_value_strategy_link_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class StrategyCardWidget extends StatelessWidget {
  final bool editable;
  final Widget editableHolderWidget;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  const StrategyCardWidget(
      {Key key,
      @required this.editable,
      @required this.editableHolderWidget,
      @required this.rolloutStrategy,
      @required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: Card(
        color: rolloutStrategy == null ? defaultValueColor : strategyValueColor,
        child: Padding(
          padding: const EdgeInsets.only(left: 8.0, right: 2.0),
          child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Expanded(
                    flex: 3,
                    child: rolloutStrategy == null
                        ? Text('default',
                            style: Theme.of(context)
                                .textTheme
                                .caption
                                .copyWith(color: defaultTextColor))
                        : EditValueStrategyLinkButton(
                            editable: editable,
                            rolloutStrategy: rolloutStrategy,
                            fvBloc: strBloc.fvBloc,
                            strBloc: strBloc,
                          )),
                Flexible(flex: 5, child: editableHolderWidget),
                Expanded(
                  flex: 2,
                  child: rolloutStrategy != null
                      ? DeleteStrategyIconButton(
                          editable: editable,
                          rolloutStrategy: rolloutStrategy,
                          strBloc: strBloc,
                        )
                      : SizedBox.shrink(),
                )
              ]),
        ),
      ),
    );
  }
}
