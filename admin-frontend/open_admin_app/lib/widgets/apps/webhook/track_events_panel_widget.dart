import 'dart:async';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';

import 'webhook_env_bloc.dart';


class TrackingEventPanelListViewWidget extends StatefulWidget {
  final WebhookEnvironmentBloc bloc;
  final String envId;
  final String cloudEventType;

  const TrackingEventPanelListViewWidget(
      {super.key, required this.bloc, required this.envId, required this.cloudEventType});

  @override
  State<StatefulWidget> createState() {
    return TrackingEventListViewState();
  }
}

class TrackingEventListViewState
    extends State<TrackingEventPanelListViewWidget> {
  static const _pageSize = 20;
  var _pageKey = 0;
  var _isLastPage = false;
  var _isLoading = false;
  Object? _error;
  final List<TrackEventsSummaryItem> _items = [];

  @override
  void initState() {
    super.initState();

    _reload();

    super.initState();
  }

  _reload() {
    _items.clear();
    _pageKey = 0;
    _fetchPage(_pageKey);
  }


  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    _reload();
  }

  Future<void> _fetchPage(int pageKey) async {
    if (_isLoading) return;

    try {
      setState(() {
        _error = null;
        _isLoading = true;
      });

      final summary =
          await widget.bloc.fetchTrackSummaryList(widget.envId, widget.cloudEventType, pageKey, _pageSize);

      setState(() {
        _isLoading = false;
        _items.addAll(summary.items);
        _isLastPage = summary.items.length < _pageSize;
      });
    } catch (error) {
      setState( () {
        _isLoading = false;
        _error = error;
      });
    } finally {

    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 14.0),
      child: Card(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(
              children: [
                const Expanded(child: Text('Activity Logs')),
                Padding(
                  padding: const EdgeInsets.only(left: 8.0),
                  child: FHFlatButton(onPressed: () => _reload(), title: 'Refresh'),
                )
              ],
            ),
          ),
          if (_error == null && _items.isEmpty && !_isLastPage && _isLoading)
            const FHLoadingIndicator(),
          if (_error == null && _items.isEmpty && _isLastPage && !_isLoading)
            const Text("There is no activity as yet."),
          for(final item in _items)
            TrackEventItemWidget(event: item),
          if (_error != null)
            const FHLoadingError(),
          if (_error == null && !_isLastPage && !_isLoading)
            _buttonRefresh('More items...'),
          if (_error != null && !_isLastPage && !_isLoading)
            _buttonRefresh('Retry...'),
          if ((_error != null || _items.isNotEmpty || _isLastPage) && _isLoading)
            const FHLoadingIndicator(),
        ],),
      ),
    );
  }

  Widget _buttonRefresh(String title) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          FHFlatButton(onPressed: () => _fetchPage(_pageKey + 1), title: 'More items...'),
        ],
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}

class TrackEventItemWidget extends StatelessWidget {
  final TrackEventsSummaryItem event;

  const TrackEventItemWidget({super.key, required this.event});

  @override
  Widget build(BuildContext context) {
    if (event.eventResponses == null) {
      return Card(
          elevation: 4.0,
          color: Colors.lightGreenAccent,
          shadowColor: Colors.transparent,
          child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  Row(
                    children: [
                      Text(
                          'Unacknowledged request sent at ${event.whenSent.toLocal()}')
                    ],
                  ),
                ],
              )));
    }

    final response = event.eventResponses!.first;
    if (_isSuccess(response)) {
      return Card(
          elevation: 4.0,
          color: Colors.lightGreenAccent,
          shadowColor: Colors.transparent,
          child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  Row(
                    children: [
                      Text(
                          'Status: ${_decodeStatus(response.status)}, received at ${response.whenReceived.toLocal()}')
                    ],
                  ),
                ],
              )));
    }


    final headers = response.headers;
    return Card(
        elevation: 4.0,
        color: Colors.orangeAccent,
        shadowColor: Colors.transparent,
        child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              children: [
                Row(
                  children: [
                    Text(
                        '${_decodeStatus(response.status)} received at ${response.whenReceived.toLocal()}'),
                  ],
                ),
                if (headers != null)
                  Row(
                    children: [
                      const Text('Response headers:'),
                      Column(
                        children: [
                          for (var header in headers.keys)
                            Text("$header: ${headers[header]}")
                        ],
                      )
                    ],
                  ),
                if (response.message != null)
                  Row(
                    children: [
                      const Text('Content'),
                      Text("${response.message}")
                    ],
                  )
              ],
            )));
  }
}

bool _isSuccess(TrackEventResponse event) {
  return (event.status >= 200 && event.status < 300);
}

String _decodeStatus(int? status) {
  if (status == null) return '';

  if (status >= 200 && status < 300) {
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
