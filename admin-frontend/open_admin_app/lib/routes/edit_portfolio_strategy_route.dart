import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/portfolio_strategies/edit_portfolio_strategy_bloc.dart';
import 'package:open_admin_app/widgets/portfolio_strategies/edit_portfolio_strategy_provider.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class EditPortfolioStrategyRoute extends StatelessWidget {
  const EditPortfolioStrategyRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditPortfolioStrategyBloc>(context);

    return Column(
      children: [
        Row(
          children: [
            FHHeader(
                title: AppLocalizations.of(context)!
                    .editPortfolioStrategyTitle(bloc.mrBloc.streamValley
                        .currentPortfolio.portfolio.name)),
          ],
        ),
        Row(
          children: [
            StreamBuilder<String?>(
                stream: bloc.mrBloc.streamValley.currentPortfolioIdStream,
                builder: (context, snapshot) {
                  if (snapshot.hasData && snapshot.data != null) {
                    return FutureBuilder(
                        future:
                            bloc.getStrategy(bloc.strId, bloc.portfolioId),
                        builder: (BuildContext context, snapshot) {
                          if (snapshot.connectionState ==
                              ConnectionState.waiting) {
                            return const FHLoadingIndicator();
                          } else if (snapshot.connectionState ==
                                  ConnectionState.active ||
                              snapshot.connectionState ==
                                  ConnectionState.done) {
                            if (snapshot.hasError) {
                              return const FHLoadingError();
                            } else if (snapshot.hasData) {
                              return BlocProvider.builder(
                                creator: (c, b) {
                                  var rs = snapshot.data;
                                  return StrategyEditorBloc(rs!.toEditing(),
                                      EditPortfolioStrategyProvider(bloc));
                                },
                                builder: (c, b) => StrategyEditingWidget(
                                  bloc: b,
                                  editable: _canEdit(bloc),
                                  returnToRoute: '/portfolio-strategies',
                                ),
                              );
                            }
                          }
                          return const SizedBox.shrink();
                        });
                  } else {
                    return const SizedBox.shrink();
                  }
                }),
          ],
        )
      ],
    );
  }

  bool _canEdit(EditPortfolioStrategyBloc bloc) {
    final rel = bloc.mrBloc.streamValley.currentPortfolio;
    return rel.currentPortfolioStrategyEditCreate ||
        rel.currentPortfolioStrategyEditor ||
        rel.currentPortfolioOrSuperAdmin;
  }
}
