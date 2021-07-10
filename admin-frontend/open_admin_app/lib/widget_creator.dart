import 'package:flutter/widgets.dart';
import 'package:open_admin_app/widgets/user/signin/signin_widget.dart';

import 'api/client_api.dart';

class WidgetCreator {
  Widget createSigninWidget(ManagementRepositoryClientBloc client) =>
      SigninWidget(client);
}

WidgetCreator widgetCreator = WidgetCreator();
