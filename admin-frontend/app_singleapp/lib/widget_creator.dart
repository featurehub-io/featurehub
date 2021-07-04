import 'package:app_singleapp/widgets/user/signin/signin_widget.dart';
import 'package:flutter/widgets.dart';

import 'api/client_api.dart';

class WidgetCreator {
  Widget createSigninWidget(ManagementRepositoryClientBloc client) =>
      SigninWidget(client);
}

WidgetCreator widgetCreator = WidgetCreator();
