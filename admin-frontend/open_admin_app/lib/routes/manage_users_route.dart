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
    final bloc = BlocProvider.of<ListPersonBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(top: 8.0),
          child: Wrap(
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
          )
        ),
        const SizedBox(height: 16.0),
        Container(
          constraints: const BoxConstraints(maxWidth: 300),
          child: TextField(
            decoration: const InputDecoration(hintText: 'Search users',
                icon: Icon(Icons.search),
                ),
            onChanged: (val) => bloc.triggerSearch(val),
          ),
        ),
        const SizedBox(height: 16.0),
        const PersonListWidget(),
      ],
    );
  }
}
