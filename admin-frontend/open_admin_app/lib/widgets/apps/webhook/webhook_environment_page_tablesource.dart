import 'dart:async';

import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_detail_table_widget.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';

class WebhookTableSource extends AdvancedDataTableSource<WebhookSummaryItem> {
  final WebhookEnvironmentBloc bloc;
  String? _filter;
  String? _envId;
  late StreamSubscription<EnvironmentAndWebhookType?> environmentSub;

  WebhookTableSource(this.bloc) {
    environmentSub = bloc.environmentStream.listen((event) {
      _envId = event.environment?.id;
      _filter = event.type?.messageType;

      setNextView();
    });
  }

  @override
  void dispose() {
    super.dispose();
    environmentSub.cancel();
  }

  set filter(String? val) {
    _filter = val;
    setNextView();
  }

  @override
  Future<RemoteDataSourceDetails<WebhookSummaryItem>> getNextPage(
      NextPageRequest pageRequest) async {
    if (_envId == null) {
      return RemoteDataSourceDetails(
        0,
        [],
        filteredRows: null, //the total amount of filtered rows, null by default
      );
    }

    final data = await bloc.mrBloc.webhookServiceApi.listWebhooks(_envId!,
        max: pageRequest.pageSize,
        startAt: pageRequest.offset,
        filter: _filter);

    return RemoteDataSourceDetails(
      data.max,
      data.results,
      filteredRows: null, //the total amount of filtered rows, null by default
    );
  }

  @override
  DataRow? getRow(int index) {
    final item = lastDetails!.rows[index];

    return DataRow.byIndex(index: index, cells: [
      DataCell(Text(item.type)),
      DataCell(Text(item.method)),
      DataCell(Builder(builder: (context) {
        final l10n = AppLocalizations.of(context)!;
        return Text(item.status == 0 ? l10n.undelivered : item.status.toString());
      })),
      DataCell(Text(item.whenSent.toIso8601String())),
      DataCell(Builder(builder: (context) {
        final l10n = AppLocalizations.of(context)!;
        return FHIconButton(
            icon: const Icon(Icons.info, color: Colors.green),
            onPressed: () {
              bloc.viewItem = item.id;
              bloc.mrBloc.addOverlay((context) => StreamBuilder<WebhookDetail?>(
                  stream: bloc.viewWebhookStream,
                  builder: (context, snapshot) {
                    if (snapshot.data == null) {
                      return const SizedBox.shrink();
                    }
                    final l10n = AppLocalizations.of(context)!;
                    return FHAlertDialog(
                      content: WebhookDetailTable(snapshot.data!, bloc),
                      title: Text(l10n.webhookDetailsTitle),
                      actions: [
                        FHFlatButton(
                            onPressed: () => bloc.viewItem = null,
                            title: l10n.close),
                      ],
                    );
                  }));
            },
            tooltip: l10n.viewWebhookDetails);
      }))
    ]);
  }

  @override
  int get selectedRowCount => 0;

  void refresh() {
    setNextView();
  }
}
