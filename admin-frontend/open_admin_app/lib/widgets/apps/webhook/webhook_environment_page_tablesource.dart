import 'dart:async';

import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
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
      DataCell(Text(item.type ?? _filter ?? '')),
      DataCell(Text(item.method)),
      DataCell(Text(item.status.toString())),
      DataCell(Text(item.whenSent.toIso8601String())),
      DataCell(FHIconButton(icon: Icon(Icons.search), onPressed: () {
        bloc.viewItem = item.id;
      }, tooltip: "View"))
    ]);
  }

  @override
  int get selectedRowCount => 0;

  refresh() {
    setNextView();
  }
}
