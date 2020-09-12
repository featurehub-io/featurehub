import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/copy_to_clipboard_html.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'per_application_features_bloc.dart';

class SDKDetailsWidget extends StatelessWidget {
  final PerApplicationFeaturesBloc bloc;
  final String envId;

  const SDKDetailsWidget({Key key, @required this.bloc, @required this.envId})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        child: Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        Tooltip(
          message: 'Copy Service Account details to use in SDK',
          verticalOffset: 12,
          child: FHIconButton(
              icon: Icon(
                Icons.settings,
                size: 14.0,
              ),
              onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                    return FHAlertDialog(
                      actions: <Widget>[
                        FHFlatButton(
                          title: 'Close',
                          onPressed: () {
                            bloc.mrClient.removeOverlay();
                          },
                        )
                      ],
                      content: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          FutureBuilder(
                              future: bloc.getEnvironment(envId),
                              builder: (BuildContext context,
                                  AsyncSnapshot<Environment> snapshot) {
                                if (snapshot.hasData &&
                                    snapshot.data.serviceAccountPermission
                                        .isNotEmpty) {
                                  return Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.stretch,
                                    children: <Widget>[
                                      for (var i in snapshot
                                          .data.serviceAccountPermission)
                                        SDKDetailsContentWidget(data: i),
                                    ],
                                  );
                                }
                                if (snapshot.hasData &&
                                    snapshot.data.serviceAccountPermission
                                        .isEmpty) {
                                  return Container(
                                      child: Text(
                                          'There are no service accounts associated with this environment.'));
                                }
                                return Container();
                              }),
                        ],
                      ),
                    );
                  })),
        ),
      ],
    ));
  }
}

class SDKDetailsContentWidget extends StatelessWidget {
  final ServiceAccountPermission data;

  const SDKDetailsContentWidget({Key key, this.data}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).scaffoldBackgroundColor,
      margin: EdgeInsets.only(top: 10.0),
      child: Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.only(left: 8.0),
              child: Text(data.serviceAccount.name,
                  style: Theme.of(context)
                      .textTheme
                      .bodyText2
                      .copyWith(fontWeight: FontWeight.bold)),
            ),
            Container(
                padding: EdgeInsets.only(left: 32.0),
                child: data.permissions.isNotEmpty
                    ? Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                              data.permissions
                                  .map((p) => p.humanReadable())
                                  .toList()
                                  .join(', '),
                              style: Theme.of(context).textTheme.bodyText1)
                        ],
                      )
                    : Text('No permissions defined',
                        style: Theme.of(context).textTheme.caption)),
            data.sdkUrl != null
                ? Padding(
                    padding: const EdgeInsets.only(left: 16.0),
                    child: FHCopyToClipboard(
                      tooltipMessage: 'Copy SDK Url to clipboard',
                      copyString: data.sdkUrl,
                    ))
                : Text('No SDK URL available')
          ]),
    );
  }
}
