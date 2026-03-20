import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_bloc.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
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
            FHHeader(
              title: AppLocalizations.of(context)!.managePortfolios,
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: AppLocalizations.of(context)!.viewDocumentation,
                  link:
                      "https://docs.featurehub.io/featurehub/latest/portfolios.html",
                  icon: const Icon(Icons.arrow_outward_outlined),
                  label: AppLocalizations.of(context)!.managePortfoliosDocumentation,
                )
              ],
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: Text(AppLocalizations.of(context)!.createNewPortfolio),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
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
                decoration: InputDecoration(
                    hintText: AppLocalizations.of(context)!.searchPortfolios, icon: const Icon(Icons.search)),
                onChanged: (val) => bloc.triggerSearch(val),
              ),
            ),
            if (bloc.mrClient.identityProviders.dacha1Enabled &&
                bloc.mrClient.personState.userIsSuperAdmin)
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Align(
                      alignment: Alignment.topRight,
                      child: OutlinedButton.icon(
                          onPressed: () => _refreshWholeCacheConfirm(bloc),
                          icon: const Icon(Icons.cached),
                          label: Text(AppLocalizations.of(context)!.republishSystemCache))),
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
      final l10n = AppLocalizations.of(context)!;
      return FHAlertDialog(
        title: Text(
          l10n.republishPortfolioCacheWarningTitle,
          style: const TextStyle(fontSize: 22.0),
        ),
        content: Text(l10n.republishEntireCacheWarningContent),
        actions: <Widget>[
          FHFlatButton(
            title: l10n.ok,
            onPressed: () {
              bloc.refreshSystemCache();
              bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
            title: l10n.cancel,
            onPressed: () {
              bloc.mrClient.removeOverlay();
            },
          )
        ],
      );
    });
  }
}
