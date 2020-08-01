import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/copy_to_clipboard_html.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'fh_alert_dialog.dart';

class FHErrorWidget extends StatefulWidget {
  final FHError error;

  const FHErrorWidget({Key key, this.error}) : super(key: key);

  @override
  _FHErrorState createState() => _FHErrorState();
}

class _FHErrorState extends State<FHErrorWidget> {
  bool showDetails = false;
  String showDetailsButton = 'View details';

  @override
  Widget build(BuildContext context) {
    return showErrorWidget(context, widget.error);
  }

  Widget showErrorWidget(context, FHError error) {
    return _showErrorAlert(context, error);
  }

  Widget _showErrorAlert(context, FHError error) {
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
              Visibility(
                  visible: error.errorMessage != '',
                  child: Text(error.errorMessage)),
              Visibility(
                  visible: error.showDetails,
                  child: Column(children: [
                    Row(
                      children: <Widget>[
                        GestureDetector(
                            child: Container(
                              padding: EdgeInsets.only(top: 20, bottom: 20),
                              child: Text(showDetailsButton,
                                  style: Theme.of(context)
                                      .textTheme
                                      .button
                                      .merge(TextStyle(
                                          color:
                                              Theme.of(context).buttonColor))),
                            ),
                            onTap: () {
                              setState(() {
                                showDetails = !showDetails;
                                showDetailsButton = showDetails
                                    ? 'Hide details'
                                    : 'View details';
                              });
                            }),
                      ],
                      mainAxisAlignment: MainAxisAlignment.start,
                    ),
                    Visibility(
                      visible: showDetails,
                      child: errorDetails(error),
                    )
                  ]))
            ],
          ),
          actions: <Widget>[
            FHFlatButtonTransparent(
                title: 'Close',
                onPressed: () {
                  //clear the error stream so we show the error only once
                  mrBloc.addError(null);
                })
          ]),
    ]);
  }

  Widget errorDetails(error) {
    return Column(
      children: <Widget>[
        Container(
          constraints:
              BoxConstraints(maxHeight: MediaQuery.of(context).size.height / 3),
          child: SingleChildScrollView(
            child: Text(
              '${error.exception.toString()}+\n\n${error.stackTrace.toString()}',
              style: TextStyle(fontFamily: 'Source', fontSize: 12),
            ),
          ),
        ),
        FHCopyToClipboardFlatButton(
            caption: ' Copy error details to clipboard',
            text:
                '${error.exception.toString()} Stack trace: ${error.stackTrace.toString()}')
      ],
    );
  }
}
