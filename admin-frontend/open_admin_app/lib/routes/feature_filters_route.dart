import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/portfolio/feature_filter_bloc.dart';

class FeatureFiltersRoute extends StatefulWidget {
  const FeatureFiltersRoute({super.key});

  @override
  State<FeatureFiltersRoute> createState() => _FeatureFiltersRouteState();
}

class _FeatureFiltersRouteState extends State<FeatureFiltersRoute> {
  late FeatureFilterBloc bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<FeatureFilterBloc>(context);
    //  the bloc isn't created every time we swap to this route so can be out of date.
    bloc.refreshFilters();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    FHAnalytics.sendScreenView("feature-filter-management");

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        FHHeader(
          title: l10n.manageFeatureFilters,
          children: [
            FHExternalLinkWidget(
              tooltipMessage: l10n.viewDocumentation,
              link: "https://docs.featurehub.io/featurehub/latest/feature-filters.html",
              icon: const Icon(Icons.arrow_outward_outlined),
              label: l10n.featureFiltersDocumentation,
            ),
          ],
        ),
        const FHPageDivider(),
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              FilledButton.icon(
                onPressed: () => _showEditDialog(context),
                icon: const Icon(Icons.add),
                label: Text(l10n.createNewFeatureFilter),
              ),
            ],
          ),
        ),
        Flexible(
          fit: FlexFit.loose,
          child: StreamBuilder<SearchFeatureFilterResult?>(
            stream: bloc.filterResultStream,
            builder: (context, snapshot) {
              if (!snapshot.hasData) {
                return const Center(child: CircularProgressIndicator());
              }

              final l10n = AppLocalizations.of(context)!;

              final filters = snapshot.data!.filters;
              if (filters.isEmpty) {
                return Center(child: Text(l10n.noFeatureFiltersFound)); // Reuse string or add new one
              }

              return SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Card(
                  child: DataTable(
                    columns: [
                      DataColumn(label: Text(l10n.columnName)),
                      DataColumn(label: Text(l10n.filterTableDescriptionLabel)),
                      DataColumn(label: Text(l10n.appsUsingFilter)),
                      DataColumn(label: Text(l10n.saUsingFilter)),
                      DataColumn(label: Text(l10n.columnActions)),
                    ],
                    rows: filters.map((filter) {
                      return DataRow(
                        cells: [
                          DataCell(Text(filter.name)),
                          DataCell(Text(filter.description ?? '')),
                          DataCell(_buildAppsUsage(filter, l10n)),
                          DataCell(_buildSAUsage(filter)),
                          DataCell(Row(
                            children: [
                              FHIconButton(
                                icon: const Icon(Icons.edit),
                                onPressed: () => _showEditDialog(context, filter: filter),
                              ),
                              FHIconButton(
                                icon: const Icon(Icons.delete),
                                onPressed: () => _showDeleteDialog(context, filter),
                              ),
                            ],
                          )),
                        ],
                      );
                    }).toList(),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildAppsUsage(SearchFeatureFilterItem filter, AppLocalizations l10n) {
    if (filter.applications == null || filter.applications!.isEmpty) {
      return const Text('-');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: filter.applications!.map((app) {
        final featureCount = app.features?.length ?? 0;

        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 2.0),
          child: Text(featureCount == 1 ? l10n.appPlusFeatureCountSingular(app.name, featureCount) : l10n.appPlusFeatureCountPlural(app.name, featureCount)),
        );
      }).toList(),
    );
  }

  Widget _buildSAUsage(SearchFeatureFilterItem filter) {
    if (filter.serviceAccounts == null || filter.serviceAccounts!.isEmpty) {
      return const Text('-');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: filter.serviceAccounts!.map((sa) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 2.0),
          child: Text(sa.name),
        );
      }).toList(),
    );
  }

  void _showEditDialog(BuildContext context, {SearchFeatureFilterItem? filter}) {
    bloc.mrClient.addOverlay((context) => FeatureFilterEditDialog(
          bloc: bloc,
          filter: filter,
        ));
  }

  void _showDeleteDialog(BuildContext context, SearchFeatureFilterItem filterItem) {
    final l10n = AppLocalizations.of(context)!;

    // We need a FeatureFilter object for the delete method
    final filter = FeatureFilter(
      id: filterItem.id,
      name: filterItem.name,
      description: filterItem.description,
      version: filterItem.version,
    );

    bloc.mrClient.addOverlay((context) => FHDeleteThingWarningWidget(
          bloc: bloc.mrClient,
          thing: filter.name,
          content: AppLocalizations.of(context)!.filterDeleteContent,
          deleteSelected: () async {
            final success = await bloc.deleteFilter(filter);
            if (success) {
              bloc.mrClient.addSnackbar(Text(l10n.filterDeleted(filter.name)));
            }
            return success;
          },
        ));
  }
}

class FeatureFilterEditDialog extends StatefulWidget {
  final FeatureFilterBloc bloc;
  final SearchFeatureFilterItem? filter;

  const FeatureFilterEditDialog({super.key, required this.bloc, this.filter});

  @override
  State<FeatureFilterEditDialog> createState() => _FeatureFilterEditDialogState();
}

class _FeatureFilterEditDialogState extends State<FeatureFilterEditDialog> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late TextEditingController _descController;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.filter?.name ?? '');
    _descController = TextEditingController(text: widget.filter?.description ?? '');
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isEditing = widget.filter != null;

    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(isEditing ? l10n.editFeatureFilter : l10n.createNewFeatureFilter),
        content: SizedBox(
          width: 500,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextFormField(
                  controller: _nameController,
                  decoration: InputDecoration(labelText: l10n.filterNameLabel),
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: (value) {
                    if (value == null || value.isEmpty) return l10n.filterNameRequired;
                    if (value.length < 4) return l10n.filterNameTooShort;
                    return null;
                  },
                ),
                TextFormField(
                  controller: _descController,
                  decoration: InputDecoration(labelText: l10n.filterDescriptionLabel),
                  validator: (value) {
                    if (value == null || value.isEmpty) return l10n.filterDescriptionRequired;
                    if (value.length < 4) return l10n.filterDescriptionTooShort;
                    return null;
                  },
                ),
              ],
            ),
          ),
        ),
        actions: [
          FHFlatButtonTransparent(
            title: l10n.cancel,
            keepCase: true,
            onPressed: () => widget.bloc.mrClient.removeOverlay(),
          ),
          FHFlatButton(
            title: isEditing ? l10n.update : l10n.create,
            keepCase: true,
            onPressed: () async {
              if (_formKey.currentState!.validate()) {
                FeatureFilter? result;
                if (isEditing) {
                  final filter = FeatureFilter(
                    id: widget.filter!.id,
                    name: widget.filter!.name,
                    description: widget.filter!.description,
                    version: widget.filter!.version,
                  );
                  result = await widget.bloc.updateFilter(
                    filter,
                    _nameController.text,
                    _descController.text,
                  );
                } else {
                  result = await widget.bloc.createFilter(
                    _nameController.text,
                    _descController.text,
                  );
                }

                if (result != null) {
                  widget.bloc.mrClient.removeOverlay();
                  widget.bloc.mrClient.addSnackbar(Text(
                    isEditing ? l10n.filterUpdated(result.name) : l10n.filterCreated(result.name),
                  ));
                }
              }
            },
          ),
        ],
      ),
    );
  }
}
