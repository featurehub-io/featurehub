import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:flutter/material.dart';

class LinkToApplicationsPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return FHFlatButtonTransparent(
        title: 'Applications',
        keepCase: true,
        onPressed: () => ManagementRepositoryClientBloc.router
            .navigateTo(context, '/applications'));
  }
}
