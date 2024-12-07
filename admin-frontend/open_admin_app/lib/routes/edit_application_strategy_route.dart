import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/application-strategies/edit_application_strategy_bloc.dart';
import 'package:open_admin_app/widgets/application-strategies/edit_application_strategy_provider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class EditApplicationStrategyRoute extends StatelessWidget {
  const EditApplicationStrategyRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditApplicationStrategyBloc>(context);

    return Column(
      children: [
        const Row(
          children: [
            FHHeader(title: "Edit Application Strategy"),
          ],
        ),
        Row(
          children: [
            StreamBuilder<String?>(
                stream: bloc.mrBloc.streamValley.currentAppIdStream,
                builder: (context, snapshot) {
                  if (snapshot.hasData && snapshot.data != null) {
                    return FutureBuilder(
                        future: bloc.getStrategy(bloc.strId),
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
                                      EditApplicationStrategyProvider(bloc));
                                },
                                builder: (c, b) => StrategyEditingWidget(
                                  bloc: b,
                                  editable: bloc.mrBloc
                                      .userHasAppStrategyEditRoleInCurrentApplication,
                                  returnToRoute: '/application-strategies',
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
}
