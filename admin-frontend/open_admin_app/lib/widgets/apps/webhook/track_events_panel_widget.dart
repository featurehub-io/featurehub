

import 'dart:async';

import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:infinite_scroll_pagination/infinite_scroll_pagination.dart';
import 'package:rxdart/rxdart.dart';

import 'webhook_env_bloc.dart';

class TrackEvent {
  final int status;

  TrackEvent(this.status);
}
class TrackEventList {
  final List<TrackEvent> trackEvents = [];
}
class TrackEventsSummaryItem {
  final int? responseStatus;
  final DateTime whenSent;
  final DateTime? whenResponseReceived;
  final String? responseMessage;

  TrackEventsSummaryItem(this.responseStatus, this.whenSent,
      this.whenResponseReceived, this.responseMessage);
}

class TrackEventsSummary {
  final int count = 0;
  final List<TrackEventsSummaryItem> items = [];
}

class TrackEventsBloc extends Bloc {
  final _trackEventSource = BehaviorSubject<TrackEventList?>.seeded(null);
  Stream<TrackEventList?> get trackEventStream => _trackEventSource.stream;
  final _trackSummary = BehaviorSubject<TrackEventsSummary?>.seeded(null);
  Stream<TrackEventsSummary?> get trackEventSummaryStream => _trackSummary.stream;
  final String cloudEventType;
  final WebhookEnvironmentBloc webhookBloc;
  EnvironmentAndWebhookType? currentEnv;
  late StreamSubscription<EnvironmentAndWebhookType> environmentChangeListener;
  int _rowCount = 5;

  TrackEventsBloc(this.cloudEventType, this.webhookBloc) {
    environmentChangeListener = webhookBloc.environmentStream.listen((env) {
      environmentChanged(env);
    });
  }

  @override
  void dispose() {
    environmentChangeListener.cancel();
    _trackEventSource.close();
  }

  void environmentChanged(EnvironmentAndWebhookType? env) async {
    // check if env has changed drop seeded ba
    if (env?.environment?.id != currentEnv?.environment?.id) {
      currentEnv = null;
      _trackEventSource.add(null);
      refreshEnvironment();
    }
  }

  set rowsPerPage(int val) {
    _rowCount = val;
    refreshEnvironment();
  }

  int get rowsPerPage => _rowCount;

  refreshEnvironment() async {

  }

  Future<TrackEventsSummary> fetchTrackSummaryList(int pageKey, int pageSize) async {
    return TrackEventsSummary();
  }
}

class TrackingEventPanelListViewWidget extends StatefulWidget {
  final TrackEventsBloc bloc;
  final EnvironmentAndWebhookType env;

  const TrackingEventPanelListViewWidget({super.key, required this.bloc, required this.env});

  @override
  State<StatefulWidget> createState() {
    return TrackingEventListViewState();
  }

}

class TrackingEventListViewState extends State<TrackingEventPanelListViewWidget> {
  static const _pageSize = 20;

  final PagingController<int, TrackEventsSummaryItem> _pagingController =
      PagingController(firstPageKey: 0);

  @override
  void initState() {
    super.initState();

    _pagingController.addPageRequestListener((pageKey) {
      _fetchPage(pageKey);
    });

    super.initState();
  }

  Future<void> _fetchPage(int pageKey) async {
    try {
      final summary = await widget.bloc.fetchTrackSummaryList(pageKey, _pageSize);
      final newItems = summary.items;
      final isLastPage = newItems.length < _pageSize;
      if (isLastPage) {
        _pagingController.appendLastPage(newItems);
      } else {
        final nextPageKey = pageKey + newItems.length;
        _pagingController.appendPage(newItems, nextPageKey);
      }
    } catch (error) {
      _pagingController.error = error;
    }
  }

  @override
  Widget build(BuildContext context) =>
  PagedListView<int, TrackEventsSummaryItem>(
    pagingController: _pagingController,
    builderDelegate: PagedChildBuilderDelegate<TrackEventsSummaryItem>(
      itemBuilder: (context, item, index) => TrackEventItemWidget(
        event: item,
      ),
    ),
  );

  @override
  void dispose() {
    _pagingController.dispose();
    super.dispose();
  }
}

class TrackEventsDetailsWidget extends StatelessWidget {
  final TrackEventsBloc bloc;

  const TrackEventsDetailsWidget({super.key, required this.bloc});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<TrackEventList?>(
        stream: bloc.trackEventStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) return SizedBox.shrink();

          final trackList = snapshot.data!.trackEvents;

          return ListView.builder(
            shrinkWrap: true,
            itemCount: trackList.length,
            itemBuilder: (context, index) {
              final row = trackList[index];
              return Card(
                  elevation: 4.0,
                  shadowColor: Colors.transparent,
                  child: Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Table(
                        border: TableBorder.all(),
                        columnWidths: const <int, TableColumnWidth>{
                          0: IntrinsicColumnWidth(),
                          1: FlexColumnWidth()
                        },
                        defaultVerticalAlignment: TableCellVerticalAlignment.middle,
                        children: <TableRow>[
                          TableRow(children: <Widget>[
                            Text("Status"),
                            Text(TrackEventsTableSource.decodeStatus(row.status))
                          ]),
                          TableRow(children: <Widget>[
                            Text("Result"),
                            Text("Some result data")
                          ]),
                          TableRow(children: <Widget>[
                            Text("Response headers"),
                            Text("Some response headers")
                          ])
                        ],
                      )

                  ));
            },

          );
        });
  }

}


   String _decodeStatus(int? status) {
    if (status == null) return '';

    if (status >= 200 && status <= 299) {
      return 'Successfully delivered';
    }
    if (status == 400) {
      return 'Undeliverable, some information missing';
    }
    if (status == 418) {
      return 'Unable to create the necessary data to send to remote system';
    }
    if (status == 422) {
      return 'Some system configuration is missing to be able to complete';
    }
    if (status == 424) {
      return 'Some system error talking to remote system (e.g. system was down)';
    }
    if (status == 500) {
      return 'Unexpected result from remote system';
    }
    if (status == 503) {
      return 'Network error, host unknown';
    }

    return 'unknown';
  }
  //
  // @override
  // DataRow? getRow(int index) {
  //   final item = lastDetails!.rows[index];
  //
  //   return DataRow.byIndex(index: index, cells: [
  //     DataCell(Text(item.whenSent.toIso8601String())),
  //     DataCell(Text(decodeStatus(item.responseStatus)),
  //     DataCell(FHIconButton(
  //         icon: const Icon(Icons.info, color: Colors.green),
  //         onPressed: () {
  //           bloc.viewItem = item.id;
  //           bloc.mrBloc.addOverlay((context) => StreamBuilder<WebhookDetail?>(
  //               stream: bloc.viewWebhookStream,
  //               builder: (context, snapshot) {
  //                 if (snapshot.data == null) {
  //                   return const SizedBox.shrink();
  //                 }
  //                 return FHAlertDialog(
  //                   content: WebhookDetailTable(snapshot.data!, bloc),
  //                   title: const Text("Details"),
  //                   actions: [
  //                     FHFlatButton(
  //                         onPressed: () => bloc.viewItem = null,
  //                         title: 'Close'),
  //                   ],
  //                 );
  //               }));
  //         },
  //         tooltip: "View webhook details"))
  //   ]);
  // }

