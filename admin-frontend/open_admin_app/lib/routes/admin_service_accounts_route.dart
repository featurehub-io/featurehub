import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/admin_sdk_service_account/list_admin_service_accounts_widget.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

class ManageAdminServiceAccountsRoute extends StatelessWidget {
  const ManageAdminServiceAccountsRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    final l10n = AppLocalizations.of(context)!;
    FHAnalytics.sendScreenView("admin-service-accounts");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            FHHeader(
              title: l10n.manageAdminSdkServiceAccounts,
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: l10n.viewDocumentation,
                  link:
                      "https://docs.featurehub.io/featurehub/latest/admin-service-accounts.html",
                  icon: const Icon(Icons.arrow_outward_outlined),
                  label: l10n.adminServiceAccountsDocumentation,
                )
              ],
            ),
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 16.0),
        if (bloc.mrClient.userIsSuperAdmin == true)
          FilledButton.icon(
            icon: const Icon(Icons.add),
            label: Text(l10n.createAdminServiceAccount),
            onPressed: () {
              ManagementRepositoryClientBloc.router
                  .navigateTo(context, '/create-admin-api-key');
            },
          ),
        const SizedBox(height: 16.0),
        widgetCreator.adminSdkBaseUrlWidget(bloc.mrClient),
        const AdminServiceAccountsListWidget(),
      ],
    );
  }
}
