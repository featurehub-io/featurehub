import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';

class LinkToApplicationsPage extends StatelessWidget {
  const LinkToApplicationsPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHUnderlineButton(
        title: 'Go to applications',
        onPressed: () => ManagementRepositoryClientBloc.router
            .navigateTo(context, '/applications'));
  }
}
