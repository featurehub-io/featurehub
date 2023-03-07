import 'dart:html';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/fh_scaffold.dart';

class SigninWrapperWidget extends StatelessWidget {
  const SigninWrapperWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final client = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    var ga = getGA();
    if(ga != null) {
      ga.sendScreenView(window.location.pathname!);
    }
    return FHScaffoldWidget(
      bodyMainAxisAlignment: MainAxisAlignment.center,
      body: Center(
          child: MediaQuery.of(context).size.width > 400
              ? SizedBox(
                  width: 500,
                  child: widgetCreator.createSigninWidget(client),
                )
              : widgetCreator.createSigninWidget(client)),
    );
  }
}
