import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/create-strategy-widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class AddStrategyButton extends StatelessWidget {
  final CustomStrategyBloc bloc;
  final bool editable;

  const AddStrategyButton({Key key, @required this.bloc, this.editable})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Container(
        height: 36,
        child: FlatButton.icon(
            label: Text('Add percentage rollout'),
            textColor: Colors.white,
            disabledColor: Colors.black12,
            color: Theme.of(context).buttonColor,
            disabledTextColor: Colors.black38,
            icon: Icon(MaterialCommunityIcons.percent,
                color: Colors.white, size: 16.0),
            onPressed: (editable == true)
                ? () => bloc.fvBloc.mrClient.addOverlay((BuildContext context) {
                      //return null;
                      return CreateValueStrategyWidget(
                        bloc: bloc,
                        editable: editable,
                      );
                    })
                : null),
      ),
    );
  }
}
