import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/create-strategy-widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class AddStrategyButton extends StatelessWidget {
  final CustomStrategyBloc bloc;
  final PerFeatureStateTrackingBloc fvBloc;
  final bool locked;

  const AddStrategyButton({Key key, this.bloc, this.fvBloc, this.locked})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Container(
        child: FlatButton.icon(
            height: 36,
            label: Text('Add percentage rollout'),
            textColor: Colors.white,
            disabledColor: Colors.black12,
            color: Theme.of(context).buttonColor,
            disabledTextColor: Colors.black38,
            icon: Icon(MaterialCommunityIcons.percent, color: Colors.white, size: 16.0),
            onPressed: (locked != true)
                ? () => fvBloc.mrClient.addOverlay((BuildContext context) {
                      //return null;
                      return CreateValueStrategyWidget(
                        fvBloc: fvBloc,
                        bloc: bloc,
                      );
                    })
                : null),
      ),
    );
  }
}
