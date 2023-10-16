import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/apps/manage_app_bloc.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_environment_widget.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';

class WebhooksPanelWidget extends StatelessWidget {
  const WebhooksPanelWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    final appBloc = BlocProvider.of<ManageAppBloc>(context);

    return StreamBuilder<List<Environment>>(
        stream: appBloc.environmentsStream,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              return BlocProvider<WebhookEnvironmentBloc>(
                  creator: (ctx, bag) =>
                      WebhookEnvironmentBloc(mrBloc, snapshot.data!, appBloc.applicationId!),
                  child: const WebhookEnvironment());
            }
          }

          return const SizedBox.shrink();
        });
  }
}
