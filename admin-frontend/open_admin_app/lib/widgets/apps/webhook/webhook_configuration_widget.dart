import 'dart:async';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

mixin WebhookEncryption {
  List<String> _encryptFields = [];

  bool isEncryptEnabled(key) {
    return _encryptFields.contains(key);
  }

  void toggleEncrypt(key) {
    if (_encryptFields.contains(key)) {
      _encryptFields.removeWhere((item) => item == key);
    } else {
      _encryptFields.add(key);
    }
  }
}

class WebhookHeader {
  String key;
  String value;

  WebhookHeader(this.key, this.value);

  @override
  String toString() {
    return 'WebhookHeader{key: $key, value: $value}';
  }
}

class _WebhookTableDataSource extends DataGridSource with WebhookEncryption {
  final List<WebhookHeader> _headers = [];
  final List<WebhookHeader> _deletedHeaders = [];
  List<DataGridRow> _rows = [];
  final bool encryptionEnabled;

  _WebhookTableDataSource(this.encryptionEnabled);

  void fillFromConfig(
      Map<String, String?> headers, String prefix, List<String> encryptFields) {
    _headers.clear();
    _encryptFields = encryptFields;
    for (var item in headers.entries) {
      _headers.add(WebhookHeader(
          Uri.decodeComponent(item.key.replaceAll("$prefix.headers.", "")),
          Uri.decodeComponent(item.value!)));
    }

    _rows = _headers
        .map((e) => DataGridRow(cells: [
              DataGridCell(columnName: 'header', value: e.key),
              DataGridCell(columnName: 'value', value: e.value),
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
          enabled: displayText != 'ENCRYPTED-TEXT',
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
  Future<void> onCellSubmit(DataGridRow dataGridRow,
      RowColumnIndex rowColumnIndex, GridColumn column) async {

    final index = dataGridRow.getCells().indexWhere(
        (DataGridCell dataGridCell) =>
            dataGridCell.columnName == column.columnName);

    if (index == -1) return;

    final oldValue = index >= 0 ? dataGridRow.getCells()[index].value : null;

    final int dataRowIndex = _rows.indexOf(dataGridRow);

    if (newCellValue == null || oldValue == newCellValue) {
      return;
    }

    rows[dataRowIndex].getCells()[index] = DataGridCell<String?>(
        columnName: column.columnName, value: newCellValue.toString());

    if (column.columnName == 'header') {
      _headers[dataRowIndex].key = newCellValue.toString();
    } else if (column.columnName == 'value') {
      _headers[dataRowIndex].value = newCellValue.toString();
    }

    // To reset the new cell value after successfully updated to DataGridRow
    //and underlying mode.
    newCellValue = null;
  }

  String encodeFromHeaders() => _headers
      .map((e) =>
          "${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}")
      .join(",");

  // Concatenates header value in the headers map with the given prefix
  Map<String, String> getHeadersMapWithPrefix(String prefix) {
    return {for (var item in _headers) "$prefix${item.key}": item.value};
  }

  // Returns all the headers that have been deleted with the deleted suffix key
  Map<String, String> getDeletedHeadersMapWithPrefix(String prefix) {
    return {
      for (var item in _deletedHeaders) "$prefix${item.key}.deleted": item.value
    };
  }

  @override
  DataGridRowAdapter? buildRow(DataGridRow row) {
    final rowIndex = _rows.indexOf(row);
    return DataGridRowAdapter(cells: [
      Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerLeft,
        child: Tooltip(
            message: "Click to edit", child: Text(row.getCells()[0].value)),
      ),
      Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerLeft,
        child: Tooltip(
            message: "Click to edit", child: Text(row.getCells()[1].value)),
      ),
      if (encryptionEnabled)
        Container(
            padding: const EdgeInsets.all(8.0),
            alignment: Alignment.centerLeft,
            child: (row.getCells()[1].value == 'ENCRYPTED-TEXT')
                ? Checkbox(
                    value: isEncryptEnabled("headers.${_headers[rowIndex].key}"),
                    onChanged: null)
                : Checkbox(
                    value: isEncryptEnabled("headers.${_headers[rowIndex].key}"),
                    onChanged: (changedValue) {
                      toggleEncrypt("headers.${_headers[rowIndex].key}");
                      notifyListeners();
                    }))
    ]);
  }

  addRow() {
    var wh = WebhookHeader('X-NewHeader', 'value');
    _headers.add(wh);
    _rows.add(DataGridRow(cells: [
      DataGridCell(columnName: 'header', value: wh.key),
      DataGridCell(columnName: 'value', value: wh.value),
    ]));
    notifyListeners();
  }

  void deleteRow(int index) {
    _rows.removeAt(index);
    _deletedHeaders.add(_headers[index]);
    _encryptFields
        .removeWhere((element) => element == "headers.${_headers[index].key}");
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
  State<WebhookConfiguration> createState() => _WebhookConfigurationState(bloc.mrBloc.identityProviders.capabilityWebhookEncryption);
}

class _WebhookConfigurationState extends State<WebhookConfiguration>
    with WebhookEncryption {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _url = TextEditingController();
  final _WebhookTableDataSource _headers;
  final DataGridController _dataGridController = DataGridController();
  final bool encryptionEnabled;
  bool enabled = false;

  // List<String> encrypt = [];

  _WebhookConfigurationState(this.encryptionEnabled):
        _headers = _WebhookTableDataSource(encryptionEnabled);

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
    final envInfo = widget.environment.webhookEnvironmentInfo ?? {};
    enabled = envInfo['${widget.type.envPrefix}.enabled'] == 'true';

    final url = envInfo['${widget.type.envPrefix}.endpoint'];

    if (url != null) {
      _url.text = url;
    } else {
      _url.text = '';
    }

    // Extract items with key starting with '${widget.type.envPrefix}.headers'
    final headers = Map.of(envInfo)
      ..removeWhere(
          (key, v) => !key.startsWith('${widget.type.envPrefix}.headers'));

    // Collect all webhook keys that are enabled to encryption
    _encryptFields = envInfo['${widget.type.envPrefix}.encrypt']
            ?.split(",")
            .map((e) => e.replaceAll("${widget.type.envPrefix}.", ''))
            .toList() ??
        [];
    _encryptFields.removeWhere((item) => item.isEmpty);

    _headers.fillFromConfig(headers, widget.type.envPrefix, _encryptFields);
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(8.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          children: [
            const Row(
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                Text('Webhook Configuration',
                    style: TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => _revert(),
                  child: const Text("Cancel"),
                ),
                FilledButton(
                    onPressed: () => _save(), child: const Text('Save')),
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
                      if (encryptionEnabled && (_url.text == 'ENCRYPTED-TEXT' ||
                          _headers._headers
                              .where((element) =>
                                  element.value == 'ENCRYPTED-TEXT')
                              .isNotEmpty))
                        TextButton.icon(
                            icon: const Icon(Icons.lock_open),
                            label: const Text("Show encrypted values"),
                            onPressed: () => fetchEncryptedText())
                    ],
                  ),
                  Row(children: [
                    Expanded(
                        child: TextFormField(
                            controller: _url,
                            autofocus: true,
                            textInputAction: TextInputAction.next,
                            readOnly: _url.text == 'ENCRYPTED-TEXT',
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
                            }))),
                    ...buildEncryptionOptions()
                  ]),
                  const SizedBox(
                    height: 24.0,
                  ),
                  Row(
                    children: [
                      TextButton.icon(
                          icon: const Icon(Icons.add),
                          label: const Text("Add HTTP Header"),
                          onPressed: () => _headers.addRow()),
                      FHIconButton(
                          tooltip: "Remove selected HTTP header",
                          icon: const Icon(Icons.delete),
                          onPressed: () => _deleteSelected()),
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
                                const DataGridCheckboxColumnSettings(
                                    showCheckboxOnHeader: false),
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
                                      child: const Text(
                                        'HTTP Header',
                                        style: TextStyle(
                                            fontWeight: FontWeight.bold),
                                      ))),
                              GridColumn(
                                  columnName: 'value',
                                  allowEditing: true,
                                  label: Container(
                                      padding: const EdgeInsets.all(8.0),
                                      alignment: Alignment.center,
                                      child: const Text('Value',
                                          style: TextStyle(
                                              fontWeight: FontWeight.bold)))),
                              if (widget.bloc.mrBloc.identityProviders
                                  .capabilityWebhookEncryption)
                                GridColumn(
                                    columnName: 'encrypt',
                                    allowEditing: true,
                                    label: Container(
                                        padding: const EdgeInsets.all(8.0),
                                        alignment: Alignment.center,
                                        child: const Text('Encrypt',
                                            style: TextStyle(
                                                fontWeight: FontWeight.bold))))
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

  List<Widget> buildEncryptionOptions() {
    if (encryptionEnabled &&
        _url.text != 'ENCRYPTED-TEXT') {
      return [
        Checkbox(
            value: isEncryptEnabled("endpoint"),
            onChanged: (_) {
              setState(() {
                toggleEncrypt("endpoint");
              });
            }),
        const Text('Encrypt URL')
      ];
    }
    return [];
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
      final envInfo = <String, String>{}
        ..addAll(widget.environment.webhookEnvironmentInfo ?? {});
      envInfo['${widget.type.envPrefix}.enabled'] = enabled.toString();
      envInfo['${widget.type.envPrefix}.endpoint'] = _url.text;

      final headers =
          _headers.getHeadersMapWithPrefix("${widget.type.envPrefix}.headers.");
      final deletedHeaders = _headers
          .getDeletedHeadersMapWithPrefix("${widget.type.envPrefix}.headers.");

      // remove all the existing header entries for this prefix from the webhookEnvInfo
      envInfo.removeWhere(
          (key, v) => key.startsWith('${widget.type.envPrefix}.headers'));

      // add all the header entries - this will have the updated headers
      envInfo.addAll(headers);
      envInfo.addAll(deletedHeaders);

      envInfo['${widget.type.envPrefix}.encrypt'] = _encryptFields
          .map((item) => '${widget.type.envPrefix}.$item')
          .join(",");

      widget.environment.webhookEnvironmentInfo = envInfo;
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
              envId: widget.environment.id,
              config: {
            '${widget.type.envPrefix}.enabled': 'true',
            '${widget.type.envPrefix}.url': _url.text
          }))
          .then((_) {
        widget.bloc.mrBloc.addSnackbar(const Text('Webhook sent.'));
      }).catchError((e, s) async {
        await widget.bloc.mrBloc.dialogError(e, s);
      });
    }
  }

  Future<void> fetchEncryptedText() async {
    await widget.bloc.decryptEncryptedFields(widget.environment);
  }
}
