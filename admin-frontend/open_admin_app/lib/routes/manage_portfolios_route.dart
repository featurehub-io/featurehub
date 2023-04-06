import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_bloc.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_widget.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.

class PortfolioRoute extends StatelessWidget {
  const PortfolioRoute({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PortfolioBloc>(context);
    FHAnalytics.sendScreenView("portfolio-management");

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          children: [
            const FHHeader(
              title: 'Manage portfolios',
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: const Text('Create new portfolio'),
                onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                  return PortfolioUpdateDialogWidget(
                    bloc: bloc,
                  );
                }),
              )
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        Row(

          children: [
            Container(
              constraints: const BoxConstraints(maxWidth: 300),
              child: TextField(
                decoration: const InputDecoration(hintText: 'Search portfolios',
                    icon: Icon(Icons.search)),
                onChanged: (val) => bloc.triggerSearch(val),
              ),
            ),
            if (bloc.mrClient.identityProviders.dacha1Enabled && bloc.mrClient.personState.userIsSuperAdmin)
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Align(alignment: Alignment.topRight,
                      child: OutlinedButton.icon(onPressed: () => _refreshWholeCacheConfirm(bloc), icon: const Icon(Icons.cached), label: const Text('Republish system cache'))),
                ),
              )
          ],
        ),
        const SizedBox(height: 16.0),
        const PortfolioListWidget(),
      ],
    );
  }

  _refreshWholeCacheConfirm(PortfolioBloc bloc) {
    bloc.mrClient.addOverlay((BuildContext context) {
      return FHAlertDialog(
        title: const Text(
          "Warning: Intensive system operation" ,
          style: TextStyle(fontSize: 22.0),
        ),
        content: const Text("Are you sure you want to republish the entire cache?"),
        actions: <Widget>[
          FHFlatButton(
            title: 'OK',
            onPressed: () {
              bloc.refreshSystemCache();
              bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
            title: 'Cancel',
            onPressed: () {
              bloc.mrClient.removeOverlay();
            },
          )
        ],
      );
    });
  }
}
