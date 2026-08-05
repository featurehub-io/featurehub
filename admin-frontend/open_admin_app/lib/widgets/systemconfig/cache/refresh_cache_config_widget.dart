import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart' show TextButton;
import 'package:mrapi/api.dart' show CacheServiceApi, CacheRefreshRequest;
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';

class RefreshCacheSystemConfigWidget extends StatefulWidget {
  const RefreshCacheSystemConfigWidget({super.key});

  @override
  State<RefreshCacheSystemConfigWidget> createState() =>
      _RefreshCacheSystemConfigWidgetState();
}

class _RefreshCacheSystemConfigWidgetState
    extends State<RefreshCacheSystemConfigWidget> {
  bool _refreshing = false;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return Stack(
      children: [
        Row(
          children: [
            Text(l10n.refreshCacheDescription),
            TextButton(
              onPressed: _refreshing ? null : () async {
                setState(() {
                  _refreshing = true;
                });
                try {
                  await CacheServiceApi(mrBloc.apiClient).cacheRefresh(CacheRefreshRequest(allTheThings: true));
                } catch (e, s) {
                  mrBloc.dialogError(e, s);
                } finally {
                  if (mounted) {
                    setState(() {
                      _refreshing = false;
                    });
                  }
                }
              },
              child: Text(l10n.refreshCacheAction),
            ),
          ],
        ),
        if (_refreshing) FHLoadingIndicator(),
      ],
    );
  }
}
