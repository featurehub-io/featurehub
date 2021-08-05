import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';
import 'package:open_admin_app/widgets/user/list/list_users_widget.dart';

class ManageUsersRoute extends StatelessWidget {
  const ManageUsersRoute({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(top: 8.0),
          child: _headerRow(context, bloc),
        ),
        const SizedBox(height: 16.0),
        _filterRow(context, bloc),
        const PersonListWidget(),
      ],
    );
  }

  Widget _headerRow(BuildContext context, ListUsersBloc bloc) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        const FHHeader(
          title: 'Manage users',
        ),
        if (bloc.mrClient.userIsSuperAdmin == true)
          Padding(
            padding: const EdgeInsets.only(top: 12.0),
            child: TextButton.icon(
              icon: const Icon(Icons.add),
              label: const Text('Create new user'),
              onPressed: () {
                ManagementRepositoryClientBloc.router
                    .navigateTo(context, '/create-user');
              },
            ),
          )
      ],
    );
  }

  Widget _filterRow(BuildContext context, ListUsersBloc bloc) {
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
              decoration: const InputDecoration(hintText: 'Filter users'),
              onChanged: (val) => bloc.triggerSearch(val),
            ),
          ),
        ],
      ),
    );
  }
}
