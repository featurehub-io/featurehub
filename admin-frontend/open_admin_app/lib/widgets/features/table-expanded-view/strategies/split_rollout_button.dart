import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_editing_widget.dart';

class AddStrategyButton extends StatelessWidget {
  final CustomStrategyBlocV2 bloc;
  final bool editable;

  const AddStrategyButton(
      {Key? key, required this.bloc, required this.editable})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
        label: const Text('Add split targeting rules'),
        icon: const Icon(MaterialCommunityIcons.arrow_split_vertical),
        onPressed: (editable == true)
            ? () => showDialog(context: context, builder: (_) {
                  return AlertDialog(
                    title: Text(editable ? 'Edit split targeting rules' : 'View split targeting rules'),
                    content: BlocProvider(
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
                    ),
                  );
                })
            : null);
  }
}
