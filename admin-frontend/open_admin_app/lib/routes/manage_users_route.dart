import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';
import 'package:open_admin_app/widgets/user/list/list_users_widget.dart';

class ManageUsersRoute extends StatelessWidget {
  const ManageUsersRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    FHAnalytics.sendWindowPath();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const FHHeader(
              title: 'Manage users',
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: const Text('Create new user'),
                onPressed: () {
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/create-user');
                },
              )
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        const PersonListWidget(),
      ],
    );
  }
}
