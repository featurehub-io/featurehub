import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/percentage_utils.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/create-strategy-widget.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditValueStrategyLinkButton extends StatelessWidget {
  const EditValueStrategyLinkButton({
    Key key,
    @required this.editable,
    @required this.fvBloc,
    @required this.rolloutStrategy,
    @required this.strBloc,
  }) : super(key: key);

  final bool editable;
  final PerFeatureStateTrackingBloc fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  @override
  Widget build(BuildContext context) {
    return FHUnderlineButton(
        enabled: editable,
        title: '${rolloutStrategy.percentageText}%',
        color: strategyTextColor,
        onPressed: editable
            ? () => {
                  fvBloc.mrClient.addOverlay((BuildContext context) {
                    //return null;
                    return CreateValueStrategyWidget(
                        bloc: strBloc,
                        rolloutStrategy: rolloutStrategy,
                        editable: editable);
                  })
                }
            : null);
  }
}
