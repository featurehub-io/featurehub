

import 'package:flutter/material.dart';
import 'package:open_admin_app/version.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:releases/api.dart';

class VersionCheckWidget extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return VersionCheckState();
  }
}

class VersionCheckState extends State<VersionCheckWidget> {
  ReleaseServiceApi _releaseServiceApi = ReleaseServiceApi(ApiClient(basePath: "https://api.dev.featurehub.io"));

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(future: _releaseServiceApi.getReleases(), builder: (ctx, snap) {
      if (snap.hasData) {
        if (snap.data?.latest != appVersion) {
          return Text('New version available v${snap.data!.latest}');
        }
      }
      return SizedBox.shrink();
    });
  }

  @override
  void initState() {
    super.initState();


  }
}
