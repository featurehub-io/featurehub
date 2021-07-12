import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/widgets.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_scaffold.dart';
import 'package:open_admin_app/widgets/setup/setup_bloc.dart';
import 'package:open_admin_app/widgets/setup/setup_widget.dart';

class SetupWrapperWidget extends StatelessWidget {
  const SetupWrapperWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final client = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return FHScaffoldWidget(
      bodyMainAxisAlignment: MainAxisAlignment.center,
      body: Center(
          child: MediaQuery.of(context).size.width > 500
              ? Container(
                  width: 500,
                  child: BlocProvider<SetupBloc>(
                      creator: (_context, _bag) => SetupBloc(client),
                      child: SetupPageWidget()),
                )
              : BlocProvider<SetupBloc>(
                  creator: (_context, _bag) => SetupBloc(client),
                  child: SetupPageWidget())),
    );
  }
}
