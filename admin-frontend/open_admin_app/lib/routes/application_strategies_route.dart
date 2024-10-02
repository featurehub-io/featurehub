import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/application-strategies/application_strategy_bloc.dart';
import 'package:open_admin_app/widgets/application-strategies/application_strategy_list.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';

class ApplicationStrategyRoute extends StatelessWidget {
  final bool createApp;
  const ApplicationStrategyRoute({Key? key, required this.createApp})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ApplicationStrategyBloc>(context);
    FHAnalytics.sendScreenView("application-strategy-list");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const FHHeader(
              title: 'Application strategies',
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: "View documentation",
                  link:
                      "https://docs.featurehub.io/featurehub/latest/application-strategies.html",
                  icon: Icon(Icons.arrow_outward_outlined),
                  label: 'Application Strategies Documentation',
                )
              ],
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: const Text('Create new strategy'),
                onPressed: () {
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/create-application-strategy');
                },
              )
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        const ApplicationStrategyList()
      ],
    );
  }
}
