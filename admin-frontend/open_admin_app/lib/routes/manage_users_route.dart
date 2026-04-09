import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';
import 'package:open_admin_app/widgets/user/list/list_users_widget.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

class ManageUsersRoute extends StatelessWidget {
  const ManageUsersRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    FHAnalytics.sendScreenView("user-management");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            FHHeader(
              title: AppLocalizations.of(context)!.manageUsers,
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: AppLocalizations.of(context)!.viewDocumentation,
                  link:
                      "https://docs.featurehub.io/featurehub/latest/users.html#_users",
                  icon: const Icon(Icons.arrow_outward_outlined),
                  label: AppLocalizations.of(context)!.manageUsersDocumentation,
                )
              ],
            ),
            if (bloc.mrClient.userIsSuperAdmin == true)
              FilledButton.icon(
                icon: const Icon(Icons.add),
                label: Text(AppLocalizations.of(context)!.createNewUser),
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
