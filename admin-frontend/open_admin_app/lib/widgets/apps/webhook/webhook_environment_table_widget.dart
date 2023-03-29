import 'package:advanced_datatable/datatable.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_configuration_widget.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_environment_page_tablesource.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';


class WebhookEnvironmentTable extends StatefulWidget {
  final WebhookEnvironmentBloc bloc;
  final EnvironmentAndWebhookType filters;

  const WebhookEnvironmentTable(this.filters, this.bloc, {Key? key}) : super(key: key);

  @override
  State<WebhookEnvironmentTable> createState() => _WebhookEnvironmentTableState();
}

class _WebhookEnvironmentTableState extends State<WebhookEnvironmentTable> {
  late WebhookTableSource tableSource;
  var rowsPerPage = 5;
  var tab = 0;

  @override
  void initState() {
    super.initState();

    tableSource = WebhookTableSource(widget.bloc);
  }

  @override
  Widget build(BuildContext context) {
    // we want a tab-bar, but its a tab-bar in a tab-bar and on a web page that doesn't layout properly,
    return Column(
      children: [
        const SizedBox(height: 24.0,),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            FHIconButton(icon: const Icon(Icons.window), tooltip: "Webhook History", onPressed: () => setState(() {
              tab = 0;
            })),
            FHIconButton(icon: const Icon(Icons.build), tooltip: "Webhook Configuration", onPressed: () => setState(() {
              tab = 1;
            }))
          ],
        ),
        if (tab == 0)
          Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  FilledButton(onPressed: () => tableSource.refresh(), child: const Text('Refresh'),)
                ],
              ),
              SelectionArea(
                child: AdvancedPaginatedDataTable(
                  rowsPerPage: rowsPerPage,
                  showCheckboxColumn: false,
                  showFirstLastButtons: true,
                  addEmptyRows: false,
                  showHorizontalScrollbarAlways: true,
                  availableRowsPerPage: const [5, 10, 20],
                  sortAscending: true,
                  sortColumnIndex: 3,
                  onRowsPerPageChanged: (newRowsPerPage) {
                    if (newRowsPerPage != null) {
                      setState(() {
                        rowsPerPage = newRowsPerPage;
                      });
                    }
                  },
                  columns: const [
                    DataColumn(label: Text('Type')),
                    DataColumn(label: Text('Method')),
                    DataColumn(label: Text('HTTP Code')),
                    DataColumn(label: Text('When Sent')),
                    DataColumn(label: Text('Actions'))
                  ],
                  source: tableSource,
                ),
              )
            ],
          ),
        if (tab == 1)
          StreamBuilder<EnvironmentAndWebhookType>(
            stream: widget.bloc.environmentStream,
            builder: (context, snapshot) {
              if (snapshot.data == null || snapshot.data!.environment == null || snapshot.data!.type == null) {
                return const SizedBox.shrink();
              }
              return WebhookConfiguration(snapshot.data!.environment!, snapshot.data!.type!, widget.bloc);
            },),
      ]);
  }
}
