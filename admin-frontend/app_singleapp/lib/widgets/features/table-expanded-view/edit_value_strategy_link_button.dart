import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/features/create-strategy-widget.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
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
        title: rolloutStrategy.name,
        onPressed: editable
            ? () => {
                  fvBloc.mrClient.addOverlay((BuildContext context) {
                    //return null;
                    return CreateValueStrategyWidget(
                      fvBloc: fvBloc,
                      bloc: strBloc,
                      rolloutStrategy: rolloutStrategy,
                    );
                  })
                }
            : null);
  }
}
