import 'package:flutter/material.dart';
import 'package:open_admin_app/version.dart';
import 'package:open_admin_app/widgets/common/fh_attention_widget.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:releases/api.dart';

class VersionCheckWidget extends StatefulWidget {
  const VersionCheckWidget({super.key});

  @override
  State<StatefulWidget> createState() {
    return VersionCheckState();
  }
}

class VersionCheckState extends State<VersionCheckWidget> {
  final ReleaseServiceApi _releaseServiceApi =
      ReleaseServiceApi(ApiClient(basePath: "https://api.dev.featurehub.io"));

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
        future: _releaseServiceApi.getReleases(),
        builder: (ctx, snap) {
          if (snap.hasData) {
            if (snap.data?.latest != appVersion) {
              return FhAttentionWidget(
                text: 'New version available v${snap.data!.latest}',
              );
            }
          }
          return const SizedBox.shrink();
        });
  }

  @override
  void initState() {
    super.initState();
  }
}
