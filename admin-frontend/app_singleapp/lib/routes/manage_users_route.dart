import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
import 'package:app_singleapp/widgets/user/list/list_users_bloc.dart';
import 'package:app_singleapp/widgets/user/list/list_users_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a super admin.
class ManageUsersRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(child: _PersonSearchWidget());
  }
}

class _PersonSearchWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _headerRow(context, bloc),
        _filterRow(context, bloc),
        PersonListWidget(),
      ],
    );
  }

  Widget _headerRow(BuildContext context, ListUsersBloc bloc) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        FHHeader(
          title: 'Manage users',
        ),
        if (bloc.mrClient.userIsSuperAdmin == true)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: FHIconTextButton(
              iconData: Icons.add,
              label: 'Create new user',
              onPressed: () {
                ManagementRepositoryClientBloc.router
                    .navigateTo(context, '/create-user');
              },
              keepCase: true,
            ),
          )
      ],
    );
  }

  Widget _filterRow(BuildContext context, ListUsersBloc bloc) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 5, 30, 10),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        border: Border(bottom: bs, left: bs, right: bs, top: bs),
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 200,
            child: TextField(
              decoration: InputDecoration(hintText: 'Filter users'),
              onChanged: (val) => bloc.triggerSearch(val),
            ),
          ),
        ],
      ),
    );
  }
}
