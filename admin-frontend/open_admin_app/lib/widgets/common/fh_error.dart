import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widget_creator.dart';

class FHErrorWidget extends StatelessWidget {
  final FHError error;

  const FHErrorWidget({Key? key, required this.error}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return Stack(children: [
      GestureDetector(
        onTap: () => mrBloc.addError(null),
        child: Container(
          color: Colors.black54,
        ),
      ),
      FHAlertDialog(
          title: Text(error.humanErrorMessage),
          content: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                if (error.errorMessage != null)
                  Visibility(
                      visible: error.errorMessage != '',
                      child: Text(error.errorMessage!)),
                widgetCreator.errorMessageDetailsWidget(fhError: error),
              ]),
          actions: <Widget>[
            FHFlatButton(
                title: 'Close',
                onPressed: () {
                  //clear the error stream so we show the error only once
                  mrBloc.addError(null);
                })
          ]),
    ]);
  }
}
