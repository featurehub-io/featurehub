import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/strategy_editing_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

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
        child: ElevatedButton.icon(
            label: Text('Add split targeting rules'),
            style: ElevatedButton.styleFrom(primary: Theme.of(context).buttonColor
    ),
            icon: Icon(MaterialCommunityIcons.arrow_split_vertical,
                color: Colors.white, size: 16.0),
            onPressed: (editable == true)
                ? () => bloc.fvBloc.mrClient.addOverlay((BuildContext context) {
                      return BlocProvider(
                        creator: (_c, _b) => IndividualStrategyBloc(
                            bloc.environmentFeatureValue, RolloutStrategy()),
                        child: StrategyEditingWidget(
                          bloc: bloc,
                          editable: editable,
                        ),
                      );
                    })
                : null),
      ),
    );
  }
}
