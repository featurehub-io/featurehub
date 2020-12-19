import 'package:app_singleapp/widgets/common/fh_underline_button.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/strategy_editing_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
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
        title: '${rolloutStrategy.name}',
        onPressed:  () => {
                  fvBloc.mrClient.addOverlay((BuildContext context) {
                    rolloutStrategy.attributes ??= [];

                    return BlocProvider(
                      creator: (_c, _b) => IndividualStrategyBloc(
                          strBloc.environmentFeatureValue, rolloutStrategy),
                      child: StrategyEditingWidget(
                          bloc: strBloc,
                          rolloutStrategy: rolloutStrategy,
                          editable: editable),
                    );
                  })
                }
            );
  }
}
