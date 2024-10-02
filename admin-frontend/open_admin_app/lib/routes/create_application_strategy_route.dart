import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/application-strategies/edit_application_strategy_bloc.dart';
import 'package:open_admin_app/widgets/application-strategies/edit_application_strategy_provider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class CreateApplicationStrategyRoute extends StatelessWidget {
  const CreateApplicationStrategyRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditApplicationStrategyBloc>(context);

    return Column(
      children: [
        const Row(
          children: [
            FHHeader(title: "Create Application Strategy"),
          ],
        ),
        Row(
          children: [
            BlocProvider.builder(
              creator: (c, b) {
                return StrategyEditorBloc(EditingRolloutStrategy.newStrategy(),
                    EditApplicationStrategyProvider(bloc));
              },
              builder: (c, b) => StrategyEditingWidget(
                bloc: b,
                editable: true,
                returnToRoute: '/application-strategies',
              ),
            ),
          ],
        )
      ],
    );
  }
}
