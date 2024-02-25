import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

class SystemConfigEncryptableMapWidget extends StatefulWidget {
  final SystemConfig field;
  final String keyHeaderName;
  final String valueHeaderName;
  final String defaultNewKeyName;
  final String defaultNewValueName;

  SystemConfigEncryptableMapWidget(
      {super.key,
      required this.field,
      required this.keyHeaderName,
      required this.valueHeaderName,
      required this.defaultNewKeyName,
      required this.defaultNewValueName});

  @override
  State<StatefulWidget> createState() {
    return SystemConfigEncryptableMapWidgetState();
  }
}

const _encryptedText = 'ENCRYPTED-TEXT';

class _SystemConfigDataSource extends DataGridSource {
  final bool decryptable;
  final String systemConfigKey;
  final Map<String, String> sourceData;
  String _encryptedKey = '';
  List<DataGridRow> _rows = [];
  List<String> encryptedRows = [];
  final String keyRowName;
  final String valueRowName;
  final SystemConfigBloc configBloc;

  _SystemConfigDataSource(this.systemConfigKey, this.decryptable,
      this.sourceData, this.keyRowName, this.valueRowName, this.configBloc) {
    _rows = sourceData.keys
        .sorted()
        .where((e) => !e.endsWith('.encrypted') )
        .map((e) => DataGridRow(cells: [
              DataGridCell(columnName: keyRowName, value: e),
              DataGridCell(columnName: valueRowName, value: sourceData[e])
            ]))
        .toList();

    _encryptedKey = sourceData.keys.firstWhere((e) => e.endsWith('.encrypted'), orElse: () => '' );
    if (_encryptedKey.isNotEmpty) {
      encryptedRows = sourceData[_encryptedKey]!.split(',');
    }
  }

  TextEditingController editingController = TextEditingController();

  ///
  dynamic newCellValue;

  @override
  List<DataGridRow> get rows {
    return _rows;
  }

  bool onCellBeginEdit(DataGridRow dataGridRow, RowColumnIndex rowColumnIndex,
      GridColumn column) {
    if (rowColumnIndex.columnIndex > 1) return false;

    final key = dataGridRow.getCells()[0].value;
    final encrypted = encryptedRows.contains(key);
    if (encrypted)  return false;

    return true;
  }

  @override
  Widget? buildEditWidget(DataGridRow dataGridRow,
      RowColumnIndex rowColumnIndex, GridColumn column, CellSubmit submitCell) {
    // these should be protected from edit mode now
    // if (rowColumnIndex.columnIndex > 1) return const SizedBox.shrink();
    // final key = dataGridRow.getCells()[0].value;
    // final encrypted = encryptedRows.contains(key);
    // if (encrypted)  return const SizedBox.shrink();

    // To set the value for TextField when cell is moved into edit mode.
    final String displayText = dataGridRow.getCells()[rowColumnIndex.columnIndex].value;

    return Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerRight,
        child: TextField(
          enabled: displayText != _encryptedText,
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
    if (rowColumnIndex.columnIndex < 0 || rowColumnIndex.columnIndex > 1)
      return;

    final oldValue = dataGridRow.getCells()[rowColumnIndex.columnIndex].value;

    if (newCellValue == null || oldValue == newCellValue) {
      return;
    }

    rows[rowColumnIndex.rowIndex].getCells()[rowColumnIndex.columnIndex] =
        DataGridCell<String?>(
            columnName: column.columnName, value: newCellValue.toString());

    if (rowColumnIndex.columnIndex == 0) {
      sourceData[newCellValue] = sourceData[oldValue]!;
      sourceData.remove(oldValue);
    } else if (column.columnName == 'value') {
      sourceData[rows[rowColumnIndex.rowIndex].getCells()[0].value] =
          newCellValue;
    }

    // To reset the new cell value after successfully updated to DataGridRow
    //and underlying mode.
    newCellValue = null;
  }

  @override
  DataGridRowAdapter? buildRow(DataGridRow row) {
    final rowIndex = _rows.indexOf(row);
    final key = row.getCells()[0].value;
    final encrypted = encryptedRows.contains(key);
    final value = encrypted ? _encryptedText : row.getCells()[1].value;
    return DataGridRowAdapter(cells: [
      Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerLeft,
        child: Tooltip(
            message: "Click to edit", child: Text(key)),
      ),
      Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerLeft,
        child: encrypted ? Text(value) : Tooltip(
            message: "Click to edit", child: Text(value)),
      ),
      Container(
          padding: const EdgeInsets.all(8.0),
          alignment: Alignment.centerLeft,
          child: _actions(
              row.getCells()[1].value, row.getCells()[0].value, rowIndex))
    ]);
  }

  Widget _actions(String? valueCell, String key, int rowIndex) {
    if (valueCell == _encryptedText && decryptable) {
      return Row(
        children: [
          FHFlatButton(
              onPressed: () => _decrypt(key, rowIndex), title: 'Decrypt'),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: FHFlatButton(
                onPressed: () => _clear(key, rowIndex), title: 'Clear'),
          ),
          FHFlatButton(onPressed: () => _delete(key, rowIndex), title: 'Delete')
        ],
      );
    }
    if (valueCell == _encryptedText && !decryptable) {
      return Row(
        children: [
          FHFlatButton(onPressed: () => _clear(key, rowIndex), title: 'Clear'),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: FHFlatButton(
                onPressed: () => _delete(key, rowIndex), title: 'Delete'),
          )
        ],
      );
    }

    return Row(children: [
      FHFlatButton(onPressed: () => _encrypt(key, rowIndex), title: 'Encrypt'),
      FHFlatButton(onPressed: () => _delete(key, rowIndex), title: 'Delete')
    ],);
  }

  _encrypt(String key, int rowIndex) async {
    encryptedRows.add(key);
    notifyListeners();
  }

  _decrypt(String key, int rowIndex) async {
    try {
      final decrypted = await configBloc.systemConfigServiceApi
          .decryptSystemConfig(systemConfigKey, mapKey: key);
      if (decrypted.result != null) {
        _rows[rowIndex].getCells()[1] =
            DataGridCell(columnName: valueRowName, value: decrypted.result ?? '');
        notifyListeners();
      }
    } catch (e) {}
  }

  _clear(String key, int rowIndex) {
    encryptedRows.remove(key);
    _rows[rowIndex].getCells()[1] =
        DataGridCell(columnName: valueRowName, value: '');
    notifyListeners();
  }

  _delete(String key, int rowIndex) {
    sourceData.remove(key);
    encryptedRows.remove(key);
    _rows.removeAt(rowIndex);
    notifyListeners();
  }

  addRow(String key, String value) {
    if (sourceData[key] != null) return;

    fhosLogger.info("adding ${key} with value ${value}");
    sourceData[key] = value;
    _rows.add(DataGridRow(cells: [
      DataGridCell(columnName: keyRowName, value: key),
      DataGridCell(columnName: valueRowName, value: value)
    ]));

    notifyListeners();
  }
}

class SystemConfigEncryptableMapWidgetState
    extends State<SystemConfigEncryptableMapWidget> {
  final DataGridController _dataGridController = DataGridController();
  late _SystemConfigDataSource _dataSource;
  late SystemConfigBloc configBloc;

  @override
  void initState() {
    super.initState();

    _setup();
  }

  @override
  void didUpdateWidget(SystemConfigEncryptableMapWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    _setup();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _setup();
  }

  _setup() {
    configBloc = BlocProvider.of(context);
    final value = Map<String,String>.from(widget.field.value
        .map((key, value) =>
        MapEntry(key.toString(), value?.toString() ?? '')));
    // put it back in, now with the correct types so it will be saved
    widget.field.value = value;
    _dataSource = _SystemConfigDataSource(
        widget.field.key,
        configBloc.mrClient.identityProviders.capabilityWebhookDecryption,
        value,
        widget.keyHeaderName,
        widget.valueHeaderName,
        configBloc);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
          border: Border.all(color: Colors.blueAccent)
      ),
      child: Column(
        children: [
          Row(children: [
            TextButton.icon(
                icon: const Icon(Icons.add),
                label: Text("Add ${widget.keyHeaderName}"),
                onPressed: () => _dataSource.addRow(
                    widget.defaultNewKeyName, widget.defaultNewValueName)),
          ]),
          Row(
            children: [
              Expanded(
                child: SfDataGrid(
                    defaultColumnWidth: 240,
                    source: _dataSource,
                    allowColumnsResizing: true,
                    allowPullToRefresh: false,
                    showCheckboxColumn: false,
                    checkboxColumnSettings: const DataGridCheckboxColumnSettings(
                        showCheckboxOnHeader: false),
                    selectionMode: SelectionMode.single,
                    navigationMode: GridNavigationMode.cell,
                    controller: _dataGridController,
                    gridLinesVisibility: GridLinesVisibility.both,
                    headerGridLinesVisibility: GridLinesVisibility.both,
                    allowEditing: true,
                    editingGestureType: EditingGestureType.tap,
                    columns: [
                      GridColumn(
                          columnName: 'header',
                          allowEditing: true,
                          label: Container(
                              // padding: EdgeInsets.all(16.0),
                              alignment: Alignment.center,
                              child: Text(
                                widget.keyHeaderName,
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ))),
                      GridColumn(
                          columnName: 'value',
                          allowEditing: true,
                          label: Container(
                              padding: const EdgeInsets.all(8.0),
                              alignment: Alignment.center,
                              child: Text(widget.valueHeaderName,
                                  style:
                                      TextStyle(fontWeight: FontWeight.bold)))),
                      GridColumn(
                          columnName: 'actions',
                          allowEditing: true,
                          label: Container(
                              padding: const EdgeInsets.all(8.0),
                              alignment: Alignment.center,
                              child: const Text('Actions',
                                  style: TextStyle(fontWeight: FontWeight.bold))))
                    ]),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
