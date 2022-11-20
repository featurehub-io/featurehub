import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_blocv2.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_editing_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class EditValueStrategyLinkButton extends StatelessWidget {
  const EditValueStrategyLinkButton({
    Key? key,
    required this.editable,
    required this.fvBloc,
    required this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool editable;
  final PerFeatureStateTrackingBlocV2 fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;

  @override
  Widget build(BuildContext context) {
    return FHUnderlineButton(
        title: rolloutStrategy.name,
        onPressed: () => {
              fvBloc.mrClient.addOverlay((BuildContext context) {
                return BlocProvider(
                  creator: (_c, _b) => IndividualStrategyBloc(
                      strBloc.environmentFeatureValue, rolloutStrategy),
                  child:
                      StrategyEditingWidget(bloc: strBloc, editable: editable),
                );
              })
            });
  }
}
