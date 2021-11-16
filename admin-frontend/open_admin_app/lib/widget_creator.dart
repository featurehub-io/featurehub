import 'package:flutter/widgets.dart';
import 'package:open_admin_app/widgets/user/signin/signin_widget.dart';

import 'api/client_api.dart';

class WidgetCreator {
  Widget createSigninWidget(ManagementRepositoryClientBloc client) =>
      SigninWidget(client);
  Widget orgNameContainer(ManagementRepositoryClientBloc client) =>
      const SizedBox.shrink();
  List<Widget> extraApplicationMenuItems(
          ManagementRepositoryClientBloc client) =>
      [];
  List<Widget> extraPortfolioMenuItems(ManagementRepositoryClientBloc client) =>
      [];
  List<Widget> extraGlobalMenuItems(ManagementRepositoryClientBloc client) =>
      [];
}

WidgetCreator widgetCreator = WidgetCreator();
