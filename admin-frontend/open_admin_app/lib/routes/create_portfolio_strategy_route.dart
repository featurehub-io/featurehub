import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/portfolio_strategies/edit_portfolio_strategy_bloc.dart';
import 'package:open_admin_app/widgets/portfolio_strategies/edit_portfolio_strategy_provider.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class CreatePortfolioStrategyRoute extends StatelessWidget {
  const CreatePortfolioStrategyRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditPortfolioStrategyBloc>(context);

    return Column(
      children: [
        Row(
          children: [
            FHHeader(
                title: AppLocalizations.of(context)!
                    .createPortfolioStrategyTitle(bloc.mrBloc.streamValley
                        .currentPortfolio.portfolio.name)),
          ],
        ),
        Row(
          children: [
            BlocProvider.builder(
              creator: (c, b) {
                return StrategyEditorBloc(EditingRolloutStrategy.newStrategy(),
                    EditPortfolioStrategyProvider(bloc));
              },
              builder: (c, b) => StrategyEditingWidget(
                bloc: b,
                editable: true,
                returnToRoute: '/portfolio-strategies',
              ),
            ),
          ],
        )
      ],
    );
  }
}
