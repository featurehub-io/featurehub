import 'package:app_singleapp/widgets/features/create-strategy-widget.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_values_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class AddStrategyButton extends StatelessWidget {
  final CustomStrategyBloc bloc;
  final FeatureValuesBloc fvBloc;
  final bool locked;

  const AddStrategyButton({Key key, this.bloc, this.fvBloc, this.locked})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      child: FlatButton.icon(
          height: 36,
          label: Text('Split rollout'),
          textColor: Colors.white,
          disabledColor: Colors.black12,
          color: Theme.of(context).buttonColor,
          disabledTextColor: Colors.black38,
          icon: Icon(AntDesign.fork, color: Colors.white, size: 16.0),
          onPressed: (locked != true)
              ? () => fvBloc.mrClient.addOverlay((BuildContext context) {
            //return null;
            return CreateValueStrategyWidget(
              fvBloc: fvBloc,
              bloc: bloc,
            );
          })
              : null
      ),
    );
  }
}
