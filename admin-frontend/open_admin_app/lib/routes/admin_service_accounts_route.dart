import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/admin_sdk_service_account/list_admin_service_accounts_widget.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class ManageAdminServiceAccountsRoute extends StatelessWidget {
  const ManageAdminServiceAccountsRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    FHAnalytics.sendScreenView("admin-service-accounts");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const FHHeader(
              title: 'Manage admin SDK service accounts',
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: const Text('Create Admin Service Account'),
                onPressed: () {
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/create-admin-api-key');
                },
              )
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        const AdminServiceAccountsListWidget(),
      ],
    );
  }
}
