import 'dart:async';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

class WebhookHeader {
  String key;
  String value;

  WebhookHeader(this.key, this.value);

  @override
  String toString() {
    return 'WebhookHeader{key: $key, value: $value}';
  }
}

class _WebhookTableDataSource extends DataGridSource {
  final List<WebhookHeader> _headers = [];
  List<DataGridRow> _rows = [];

  void fillFromConfig(String? headerStr) {
    _headers.clear();

    headerStr?.split(",").forEach((header) {
      header = header.trim();
      if (header.isNotEmpty) {
        final parts = header.split("=");
        if (parts.length == 2) {
          _headers.add(WebhookHeader(
              Uri.decodeComponent(parts[0]), Uri.decodeComponent(parts[1])));
        }
      }
    });

    _rows = _headers
        .map((e) => DataGridRow(cells: [
              DataGridCell(columnName: 'header', value: e.key),
              DataGridCell(columnName: 'value', value: e.value)
            ]))
        .toList();
  }

  @override
  List<DataGridRow> get rows {
    return _rows;
  }

  TextEditingController editingController = TextEditingController();

  ///
  dynamic newCellValue;

  ///
  @override
  Widget? buildEditWidget(DataGridRow dataGridRow,
      RowColumnIndex rowColumnIndex, GridColumn column, CellSubmit submitCell) {
    // To set the value for TextField when cell is moved into edit mode.
    final String displayText = dataGridRow
            .getCells()
            .firstWhere((DataGridCell dataGridCell) =>
                dataGridCell.columnName == column.columnName)
            .value
            ?.toString() ??
        '';

    return Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerRight,
        child: TextField(
          autofocus: true,
          controller: editingController..text = displayText,
          textAlign: TextAlign.left,
          decoration: const InputDecoration(
              contentPadding: EdgeInsets.all(8),
              // border: InputBorder.none,
              isDense: true),
          keyboardType: TextInputType.text,
          onChanged: (String value) {
            newCellValue = value;
          },
          onSubmitted: (String value) {
            submitCell();
          },
        ));
  }

  @override
  void onCellSubmit(DataGridRow dataGridRow, RowColumnIndex rowColumnIndex,
      GridColumn column) {

    final index = dataGridRow
        .getCells()
        .indexWhere((DataGridCell dataGridCell) =>
          dataGridCell.columnName == column.columnName);

    final oldValue = index >= 0 ? dataGridRow.getCells()[index].value : null;

    final int dataRowIndex = _rows.indexOf(dataGridRow);

    if (newCellValue == null || oldValue == newCellValue) {
      return;
    }

    rows[dataRowIndex].getCells()[index] =
        DataGridCell<String?>(columnName: column.columnName, value: newCellValue.toString());

    if (column.columnName == 'header') {
      _headers[dataRowIndex].key = newCellValue.toString();
    } else {
      _headers[dataRowIndex].value = newCellValue.toString();
    }

    // To reset the new cell value after successfully updated to DataGridRow
    //and underlying mode.
    newCellValue = null;
  }

  String encodeFromHeaders() => _headers
      .map((e) =>
          Uri.encodeComponent(e.key) + "=" + Uri.encodeComponent(e.value))
      .join(",");

  @override
  DataGridRowAdapter? buildRow(DataGridRow row) {
    return DataGridRowAdapter(
        cells: row
            .getCells()
            .map((e) =>
                Container(
                  padding: const EdgeInsets.all(8.0),
                    alignment: Alignment.centerLeft, child: Tooltip(message: "Click to edit",child: Text(e.value))))
            .toList());
  }

  addRow() {
    var wh = WebhookHeader('X-NewHeader', 'value');
    _headers.add(wh);
    _rows.add(DataGridRow(cells: [
      DataGridCell(columnName: 'header', value: wh.key),
      DataGridCell(columnName: 'value', value: wh.value)
    ]));
    notifyListeners();
  }

  void deleteRow(int index) {
    _rows.removeAt(index);
    _headers.removeAt(index);
    notifyListeners();
  }
}

class WebhookConfiguration extends StatefulWidget {
  final Environment environment;
  final WebhookTypeDetail type;
  final WebhookEnvironmentBloc bloc;

  const WebhookConfiguration(this.environment, this.type, this.bloc, {Key? key})
      : super(key: key);

  @override
  State<WebhookConfiguration> createState() => _WebhookConfigurationState();
}

class _WebhookConfigurationState extends State<WebhookConfiguration> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _url = TextEditingController();
  final _WebhookTableDataSource _headers = _WebhookTableDataSource();
  final DataGridController _dataGridController = DataGridController();
  bool enabled = false;

  @override
  void initState() {
    super.initState();

    _setup();
  }

  // each time we swap the entry in the textbox (update the environment), this gets called because
  // the base WebhookConfiguration widget has changed but this UI hasn't.
  @override
  void didUpdateWidget(WebhookConfiguration oldWidget) {
    super.didUpdateWidget(oldWidget);

    _setup();
  }

  void _setup() {
    enabled = widget
            .environment.environmentInfo['${widget.type.envPrefix}.enabled'] ==
        'true';

    final url =
        widget.environment.environmentInfo['${widget.type.envPrefix}.endpoint'];

    if (url != null) {
      _url.text = url;
    } else {
      _url.text = '';
    }

    final headerStr =
        widget.environment.environmentInfo['${widget.type.envPrefix}.headers'];

    _headers.fillFromConfig(headerStr);
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(8.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.start,
              children: const [
                Text('Webhook Configuration', style:
                TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                    onPressed: () => _revert(),
                    child: const Text("Cancel"),),
                FilledButton(
                    onPressed: () => _save(),
                    child: const Text('Save')),
                if (enabled && _url.text.isNotEmpty)
                  FHIconButton(
                      icon: const Icon(Icons.send),
                      onPressed: () => _testWebhook(),
                      tooltip: 'Test Webhook'),
              ],
            ),
            Form(
                key: _formKey,
                child:
                    Column(mainAxisSize: MainAxisSize.min, children: <Widget>[
                  Row(
                    children: [
                      Checkbox(
                          value: enabled,
                          onChanged: (_) {
                            setState(() {
                              enabled = !enabled;
                            });
                          }),
                      const Text('Enabled'),
                    ],
                  ),
                  TextFormField(
                      controller: _url,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      decoration:
                          const InputDecoration(labelText: 'Webhook URL'),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter a valid webhook URL';
                        }
                        if (!v.startsWith("http://") &&
                            v.startsWith("https://")) {
                          return 'Please enter a valid URL';
                        }
                        return null;
                      })),
                  const SizedBox(height: 24.0,),
                  Row(
                    children: [
                      TextButton.icon(icon: const Icon(Icons.add), label: const Text("Add HTTP Header"), onPressed: () => _headers.addRow()),
                      FHIconButton(tooltip: "Remove selected HTTP header", icon: const Icon(Icons.delete), onPressed: () => _deleteSelected()),
                    ],
                  ),
                  Row(
                    children: [
                      Expanded(
                        child: SfDataGrid(
                          defaultColumnWidth: 240,
                            source: _headers,
                            allowColumnsResizing: true,
                            allowPullToRefresh: false,
                            showCheckboxColumn: true,
                            checkboxColumnSettings:
                            const DataGridCheckboxColumnSettings(showCheckboxOnHeader: false),
                            selectionMode: SelectionMode.single,
                            navigationMode: GridNavigationMode.cell,
                            controller: _dataGridController,
                            gridLinesVisibility: GridLinesVisibility.both,
                            headerGridLinesVisibility: GridLinesVisibility.both,
                            allowEditing: true,
                            columns: [
                              GridColumn(
                                  columnName: 'header',
                                  allowEditing: true,
                                  label: Container(
                                      // padding: EdgeInsets.all(16.0),
                                      alignment: Alignment.center,
                                      child: const Text('HTTP Header', style: TextStyle(fontWeight: FontWeight.bold),))),
                              GridColumn(
                                  columnName: 'value',
                                  allowEditing: true,
                                  label: Container(
                                      padding: const EdgeInsets.all(8.0),
                                      alignment: Alignment.center,
                                      child: const Text('Value', style: TextStyle(fontWeight: FontWeight.bold))))
                            ]),
                      ),
                    ],
                  )
                ])),
          ],
        ),
      ),
    );
  }

  void _deleteSelected() {
    if (_dataGridController.selectedIndex != -1) {
      _headers.deleteRow(_dataGridController.selectedIndex);
    }
  }

  void _revert() {
    setState(() => _setup());
  }

  Future<void> _save() async {
    if (_formKey.currentState!.validate()) {
      // make sure the map is modifiable
      widget.environment.environmentInfo = {}..addAll(widget.environment.environmentInfo);
      widget.environment.environmentInfo['${widget.type.envPrefix}.enabled'] =
          enabled.toString();
      widget.environment.environmentInfo['${widget.type.envPrefix}.endpoint'] =
          _url.text;
      widget.environment.environmentInfo['${widget.type.envPrefix}.headers'] =
          _headers.encodeFromHeaders();

      widget.bloc.updateEnvironment(widget.environment).then((_) {
        widget.bloc.mrBloc.addSnackbar(Text(
            "Environment '${widget.environment.name}' updated with webhook details!"));
      }).catchError((e, s) async {
        await widget.bloc.mrBloc.dialogError(e, s);
      });
    }
  }

  Future<void> _testWebhook() async {
    if (_formKey.currentState!.validate()) {
      await widget.bloc
          .sendWebhookCheck(WebhookCheck(
              messageType: widget.type.messageType,
              envId: widget.environment.id!,
              config: {
            '${widget.type.envPrefix}.enabled': 'true',
            '${widget.type.envPrefix}.url': _url.text
          }))
          .then((_) {
        widget.bloc.mrBloc.addSnackbar(const Text(
            'Webhook sent.'));
      }).catchError((e, s) async {
        await widget.bloc.mrBloc.dialogError(e, s);
      });
    }
  }
}
