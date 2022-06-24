import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/admin_sdk_service_account/list_admin_service_accounts_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class ManageAdminServiceAccountsRoute extends StatelessWidget {
  const ManageAdminServiceAccountsRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListAdminServiceAccount>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(top: 8.0),
          child: _headerRow(context, bloc),
        ),
        const SizedBox(height: 16.0),
        _filterRow(context, bloc),
        const AdminServiceAccountsListWidget(),
      ],
    );
  }

  Widget _headerRow(BuildContext context, ListAdminServiceAccount bloc) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        const FHHeader(
          title: 'Manage admin SDK service accounts',
        ),
        if (bloc.mrClient.userIsSuperAdmin == true)
          Padding(
            padding: const EdgeInsets.only(top: 12.0),
            child: TextButton.icon(
              icon: const Icon(Icons.add),
              label: const Text('Create Admin Service Account'),
              onPressed: () {
                ManagementRepositoryClientBloc.router
                    .navigateTo(context, '/create-admin-api-key');
              },
            ),
          )
      ],
    );
  }

  Widget _filterRow(BuildContext context, ListAdminServiceAccount bloc) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    return Container(
      padding: const EdgeInsets.fromLTRB(30, 10, 30, 10),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        border: Border(bottom: bs, left: bs, right: bs, top: bs),
      ),
      child: Row(
        children: <Widget>[
          SizedBox(
            width: 200,
            child: TextField(
              decoration: const InputDecoration(hintText: 'Filter Service Accounts'),
              onChanged: (val) => bloc.triggerSearch(val),
            ),
          ),
        ],
      ),
    );
  }
}
