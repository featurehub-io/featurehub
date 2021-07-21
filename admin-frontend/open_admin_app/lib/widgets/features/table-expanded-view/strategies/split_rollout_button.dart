import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_editing_widget.dart';

class AddStrategyButton extends StatelessWidget {
  final CustomStrategyBloc bloc;
  final bool editable;

  const AddStrategyButton(
      {Key? key, required this.bloc, required this.editable})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Container(
        height: 36,
        child: TextButton.icon(
            label: const Text('Add split targeting rules'),
            icon: const Icon(MaterialCommunityIcons.arrow_split_vertical),
            onPressed: (editable == true)
                ? () => bloc.fvBloc.mrClient.addOverlay((BuildContext context) {
                      return BlocProvider(
                        creator: (_c, _b) => IndividualStrategyBloc(
                            bloc.environmentFeatureValue,
                            RolloutStrategy(
                              name: '',
                              id: 'created',
                            )),
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
