import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

typedef SystemUpdateConfigCallback = void Function(SystemConfig);

//the parent widget needs to pass this in as a controller style so
//when it is due to submit/validate, the state of the map can be fixed up.

class SystemConfigEncryptionController {
  Function? submitCallback;
  SystemUpdateConfigCallback? updateConfigCallback;

  updateField(SystemConfig config) {
    updateConfigCallback?.call(config);
  }

  submit() {
    submitCallback?.call();
  }
}

class SystemConfigEncryptableMapWidget extends StatefulWidget {
  final SystemConfig field;
  final String keyHeaderName;
  final String valueHeaderName;
  final String defaultNewKeyName;
  final String defaultNewValueName;
  final SystemConfigEncryptionController controller;

  SystemConfigEncryptableMapWidget(
      {super.key,
      required this.field,
      required this.keyHeaderName,
      required this.valueHeaderName,
      required this.defaultNewKeyName,
      required this.defaultNewValueName,
      required this.controller});

  @override
  State<StatefulWidget> createState() {
    return SystemConfigEncryptableMapWidgetState();
  }
}

const _encryptedText = 'ENCRYPTED-TEXT';

class _SystemConfigDataSource extends DataGridSource {
  final bool decryptable;
  final String systemConfigKey;
  late Map<String, String> sourceData;
  // this key tracks that which refers to encrypted fields - it ends in .encrypt
  String _encryptedKey = '';
  // this key tracks which fields were deleted, we need to pass them back so the backend knows to remove the keys
  String _deletedKey = '';
  List<DataGridRow> _rows = [];
  List<String> encryptedRows = [];
  List<String> deletedRows = [];
  final String keyRowName;
  final String valueRowName;
  final SystemConfigBloc configBloc;

  _SystemConfigDataSource(this.systemConfigKey, this.decryptable,
      dynamic fieldValue, this.keyRowName, this.valueRowName, this.configBloc) {
    _reset(fieldValue);
  }

  _reset(dynamic fieldValue) {
    sourceData = Map<String, String>.from(fieldValue.map(
        (key, value) => MapEntry(key.toString(), value?.toString() ?? '')));

    _rows = sourceData.keys
        .sorted()
        .where((e) => !e.endsWith('.encrypt') && !e.endsWith(".deleted"))
        .map((e) => DataGridRow(cells: [
              DataGridCell(columnName: keyRowName, value: e),
              DataGridCell(columnName: valueRowName, value: sourceData[e])
            ]))
        .toList();

    _encryptedKey = '$systemConfigKey.encrypt';
    encryptedRows = sourceData[_encryptedKey]
            ?.split(',')
            .whereNot((k) => k.isEmpty)
            .toList() ??
        [];

    fhosLogger.info("reset to $sourceData, e-rows $encryptedRows");

    // prefill
    _deletedKey = '$systemConfigKey.deleted';
    deletedRows.clear();
  }

  submit() {
    fhosLogger.info(
        "sourceData is $sourceData, e-rows $encryptedRows, d-rows $deletedRows");
    var rows = deletedRows
        .where((k) => sourceData[k] == null)
        .where((k) => k.isNotEmpty);
    if (rows.isNotEmpty) {
      sourceData[_deletedKey] = rows.join(',');
    }
    sourceData[_encryptedKey] =
        encryptedRows.where((k) => k.isNotEmpty).join(",");
  }

  TextEditingController editingController = TextEditingController();

  ///
  dynamic newCellValue;

  @override
  List<DataGridRow> get rows {
    return _rows;
  }

  @override
  bool onCellBeginEdit(DataGridRow dataGridRow, RowColumnIndex rowColumnIndex,
      GridColumn column) {
    if (rowColumnIndex.columnIndex > 1) return false;

    // can't rename encrypted headers or edit them
    final key = dataGridRow.getCells()[0].value;
    final encrypted = encryptedRows.contains(key);
    if (encrypted) return false;

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
    final String displayText =
        dataGridRow.getCells()[rowColumnIndex.columnIndex].value;

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
    if (rowColumnIndex.columnIndex < 0 || rowColumnIndex.columnIndex > 1) {
      return;
    }

    final oldValue = dataGridRow.getCells()[rowColumnIndex.columnIndex].value;

    // nothing changed or the new cell value is null, so bail
    if (newCellValue == null || oldValue == newCellValue) {
      return;
    }

    rows[rowColumnIndex.rowIndex].getCells()[rowColumnIndex.columnIndex] =
        DataGridCell<String?>(
            columnName: column.columnName, value: newCellValue.toString());

    if (rowColumnIndex.columnIndex == 0) {
      // copy old cell to new cell
      sourceData[newCellValue] = sourceData[oldValue]!;
      // remove old cell
      sourceData.remove(oldValue);
      if (deletedRows.contains(newCellValue)) {
        deletedRows.remove(newCellValue);
      }
    } else if (column.columnName == 'value') {
      sourceData[rows[rowColumnIndex.rowIndex].getCells()[0].value] =
          newCellValue;
    }

    fhosLogger.info("sourceMap is now $sourceData, e-rows $encryptedRows");

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
        child: Tooltip(message: "Click to edit", child: Text(key)),
      ),
      Container(
        padding: const EdgeInsets.all(8.0),
        alignment: Alignment.centerLeft,
        child: encrypted
            ? Text(value)
            : Tooltip(message: "Click to edit", child: Text(value)),
      ),
      Container(
          padding: const EdgeInsets.all(8.0),
          alignment: Alignment.centerLeft,
          child: _actions(rowIndex))
    ]);
  }

  Widget _actions(int rowIndex) {
    String? valueCell = rows[rowIndex].getCells()[1].value;
    DataGridCell keyCell = rows[rowIndex].getCells()[0];

    if (valueCell != _encryptedText && encryptedRows.contains(keyCell.value)) {
      return Row(
        children: [
          TextButton(
              onPressed: () => _reveal(rowIndex), child: const Text('Show')),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextButton(
                onPressed: () => _clear(rowIndex), child: const Text('Clear')),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextButton(
                onPressed: () => _delete(rowIndex),
                child: const Text('Delete')),
          )
        ],
      );
    }

    if (encryptedRows.contains(keyCell.value) && decryptable) {
      return Row(
        children: [
          TextButton(
            onPressed: () => _decrypt(rowIndex),
            child: const Text('Decrypt'),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextButton(
              onPressed: () => _clear(rowIndex),
              child: const Text('Clear'),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextButton(
                onPressed: () => _delete(rowIndex),
                child: const Text('Delete')),
          )
        ],
      );
    }
    if (valueCell == _encryptedText && !decryptable) {
      return Row(
        children: [
          TextButton(
              onPressed: () => _clear(rowIndex), child: const Text('Clear')),
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextButton(
                onPressed: () => _delete(rowIndex),
                child: const Text('Delete')),
          )
        ],
      );
    }

    return Row(
      children: [
        TextButton(
          onPressed: () => _encrypt(rowIndex),
          child: const Text('Encrypt'),
        ),
        Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: TextButton(
            onPressed: () => _delete(rowIndex),
            child: const Text('Delete'),
          ),
        )
      ],
    );
  }

  _addEncryptedKey(String key) {
    if (key.isNotEmpty) {
      encryptedRows.add(key);
    }
    fhosLogger.info(", e-rows $encryptedRows");
  }

  // we have to do it this way, getting the key at the last second as it may change
  _key(int rowIndex) {
    return rows[rowIndex].getCells()[0].value;
  }

  _removeEncryptedKey(String key) {
    encryptedRows.remove(key);
  }

  _reveal(int rowIndex) async {
    _removeEncryptedKey(_key(rowIndex));
    notifyListeners();
  }

  _encrypt(int rowIndex) async {
    _addEncryptedKey(_key(rowIndex));
    notifyListeners();
  }

  _decrypt(int rowIndex) async {
    String key = _key(rowIndex);
    try {
      final decrypted = await configBloc.systemConfigServiceApi
          .decryptSystemConfig(systemConfigKey, mapKey: key);
      if (decrypted.result != null) {
        _removeEncryptedKey(key);
        _rows[rowIndex].getCells()[1] = DataGridCell(
            columnName: valueRowName, value: decrypted.result ?? '');
        notifyListeners();
      }
    } catch (e) {}
  }

  _clear(int rowIndex) {
    _removeEncryptedKey(_key(rowIndex));
    _rows[rowIndex].getCells()[1] =
        DataGridCell(columnName: valueRowName, value: '');
    notifyListeners();
  }

  _delete(int rowIndex) {
    String key = _key(rowIndex);
    sourceData.remove(key);
    _removeEncryptedKey(key);
    _rows.removeAt(rowIndex);
    deletedRows.add(key);
    notifyListeners();
  }

  addRow(String key, String value) {
    if (sourceData[key] != null) return;

    fhosLogger.info("adding $key with value $value");
    sourceData[key] = value;
    _rows.add(DataGridRow(cells: [
      DataGridCell(columnName: keyRowName, value: key),
      DataGridCell(columnName: valueRowName, value: value)
    ]));

    notifyListeners();
  }

  Map<String, String> changeConfig(SystemConfig config) {
    _reset(config.value);
    notifyListeners();
    return sourceData;
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

  submit() {
    _dataSource.submit();
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

    // this converts it from a Map<dynamic,dynamic> to a Map<String,String>, which is a wee bit annoying but...
    _dataSource = _SystemConfigDataSource(
        widget.field.key,
        configBloc.mrClient.identityProviders.capabilityWebhookDecryption,
        widget.field.value,
        widget.keyHeaderName,
        widget.valueHeaderName,
        configBloc);

    // put it back in, now with the correct types so it will be saved
    widget.field.value = _dataSource.sourceData;

    widget.controller.submitCallback = () => _dataSource.submit();
    widget.controller.updateConfigCallback =
        (config) => _dataSource.changeConfig(config);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(border: Border.all(color: Colors.blueAccent)),
      child: Column(
        children: [
          Row(children: [
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: TextButton.icon(
                  icon: const Icon(Icons.add),
                  label: Text("Add ${widget.keyHeaderName}"),
                  onPressed: () => _dataSource.addRow(
                      widget.defaultNewKeyName, widget.defaultNewValueName)),
            ),
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
                    checkboxColumnSettings:
                        const DataGridCheckboxColumnSettings(
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
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold),
                              ))),
                      GridColumn(
                          columnName: 'value',
                          allowEditing: true,
                          label: Container(
                              padding: const EdgeInsets.all(8.0),
                              alignment: Alignment.center,
                              child: Text(widget.valueHeaderName,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold)))),
                      GridColumn(
                          columnName: 'actions',
                          allowEditing: false,
                          columnWidthMode: ColumnWidthMode.fill,
                          label: Container(
                              padding: const EdgeInsets.all(8.0),
                              alignment: Alignment.center,
                              child: const Text('Actions',
                                  style:
                                      TextStyle(fontWeight: FontWeight.bold))))
                    ]),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
